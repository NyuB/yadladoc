package nyub.yadladoc

import nyub.markdown.Markdown
import nyub.yadladoc.Yadladoc.DocumentationKind
import nyub.filesystem.{
    /,
    useLines,
    FileSystem,
    FileTree,
    InMemoryFileSystem,
    OsFileSystem
}
import nyub.templating.{SurroundingTemplateInjection, TemplateInjection}

import java.nio.file.{Path, Paths}

class Yadladoc(
    private val config: Yadladoc.Configuration,
    private val fileSystem: FileSystem = OsFileSystem()
):
    private val templating: TemplateInjection = SurroundingTemplateInjection(
      config.templateInjectionPrefix,
      config.templateInjectionPostfix
    )

    def run(outputDir: Path, markdownFile: Path): Iterable[GeneratedFile] =
        run(outputDir, markdownFile, fileSystem)

    def check(outputDir: Path, markdownFile: Path): List[Errors] =
        val checkFs = InMemoryFileSystem.init()
        val checkDir = checkFs.createTempDirectory("check")

        val generated = run(checkDir, markdownFile, checkFs)
        generated
            .map(checkGeneratedFile(_, outputDir, checkFs, checkDir))
            .flatMap(_.toList)
            .toList

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Iterable[GeneratedFile] =
        val examples = snippets(markdownFile)
            .foldLeft(ExampleSnippetMerger.empty(config)):
                (examples, snippet) =>
                    config.exampleForSnippet(snippet) match
                        case example: DocumentationKind.ExampleSnippet =>
                            examples.accumulate(example)
                        case DocumentationKind.Ignore =>
                            examples // no doc to generate
            .examples

        for example <- examples.values yield
            val fullExample = buildFullExample(example)
            val exampleFile = config.exampleFile(example.name, example.language)

            writeFs.writeContent(
              outputDir / exampleFile,
              fullExample.mkString("\n")
            )

            GeneratedFile(exampleFile, markdownFile)

    private def buildFullExample(example: Example) =
        example.build(
          templating,
          id => config.templateFile(id).useLines(_.toList),
          config.snippetInjectionKey,
          config.exampleNameInjectionKey,
          config.subExampleNameInjectionKey,
          config.properties.toMap
        )

    private def snippets(markdownFile: Path): Iterable[Snippet] = markdownFile
        .useLines(Markdown.parse(_))
        .collect:
            case s: Markdown.Snippet =>
                Snippet(
                  s.header.language,
                  s.lines,
                  Properties.ofLine(s.header.afterLanguage)
                )

    private def checkGeneratedFile(
        generated: GeneratedFile,
        outputDir: Path,
        checkFs: FileSystem,
        checkDir: Path
    ): Option[Errors] =
        val actualFile = fileSystem.toFileTree(outputDir / generated.file)
        val checkFile = checkFs.toFileTree(checkDir / generated.file)

        actualFile -> checkFile match
            case _ -> None =>
                throw IllegalStateException(
                  "Generated files should exist in check file system"
                )
            case _ -> Some(FileTree.Dir(_)) =>
                throw IllegalStateException(
                  "Generated files should not be a directory in check file system"
                )
            case actual -> Some(FileTree.File(expected)) =>
                actual match
                    case None => Some(CheckErrors.MissingFile(generated.file))
                    case Some(FileTree.Dir(_)) =>
                        Some(CheckErrors.MissingFile(generated.file))
                    case Some(FileTree.File(af)) =>
                        val actualContent = fileSystem.content(af)
                        val expectedContent =
                            checkFs.content(expected)
                        if actualContent == expectedContent then None
                        else
                            Some(
                              CheckErrors.MismatchingContent(
                                generated.file,
                                actualContent,
                                expectedContent
                              )
                            )

object Yadladoc:
    val DEFAULT_LANGUAGE = Language.named("default")
    trait Configuration:
        def properties: Properties
        def configDir: Path
        def includesDir: Path = properties.getPathOrDefault("ydoc.includesDir")(
          configDir / "includes"
        )

        def templateFile(templateId: TemplateId): Path =
            includesDir / s"${templateId}.template"

        def templateId(
            language: Option[Language],
            properties: Properties
        ): TemplateId =
            val id = properties.getOrDefault("ydoc.template")(
              language.getOrElse(DEFAULT_LANGUAGE).name
            )
            TemplateId(id)

        def templateInjectionPrefix =
            properties.getOrDefault("ydoc.templateInjectionPrefix")("${{")

        def templateInjectionPostfix =
            properties.getOrDefault("ydoc.templateInjectionPostfix")("}}")

        def snippetInjectionKey: String =
            properties.getOrDefault("ydoc.snippetInjectionKey")("ydoc.snippet")

        def exampleNameInjectionKey: String =
            properties.getOrDefault("ydoc.exampleNameInjectionKey")(
              "ydoc.exampleName"
            )

        def exampleNamePropertyKey: String = properties.getOrDefault(
          "ydoc.exampleNamePropertyKey"
        )("ydoc.example")

        def subExampleNameInjectionKey: String = properties.getOrDefault(
          "ydoc.subExampleNamePropertyKey"
        )("ydoc.subExampleName")

        def exampleForSnippet(snippet: Snippet): DocumentationKind =
            snippet.properties
                .get(exampleNamePropertyKey)
                .filterNot(_.isBlank)
                .map(DocumentationKind.ExampleSnippet(_, snippet))
                .getOrElse(DocumentationKind.Ignore)

        def exampleFile(
            exampleName: String,
            exampleLanguage: Option[Language]
        ): Path =
            Paths.get(
              s"${exampleName}"
            )

        def prefixTemplateIds(
            snippet: Snippet
        ): Iterable[TemplateId] =
            snippet.properties.get("ydoc.prefix").toList.map(TemplateId(_))

        def suffixTemplateIds(
            snippet: Snippet
        ): Iterable[TemplateId] =
            snippet.properties.get("ydoc.suffix").toList.map(TemplateId(_))

    enum DocumentationKind:
        case ExampleSnippet(val name: String, val snippet: Snippet)
        case Ignore

    case class ConfigurationFromFile(
        override val configDir: Path,
        private val storage: FileSystem = OsFileSystem()
    ) extends Configuration:
        override val properties: Properties =
            val propertyFile = configDir / "ydoc.properties"
            if !propertyFile.toFile().isFile() then Properties.empty
            else
                storage.useLines(propertyFile): lines =>
                    lines.foldLeft(Properties.empty): (props, line) =>
                        props.extendedWith(Properties.ofLine(line))

private class ExampleSnippetMerger(
    val config: Yadladoc.Configuration,
    val examples: Map[String, Example]
):
    def accumulate(
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): ExampleSnippetMerger =
        val updatedExamples = examples.updatedWith(exampleSnippet.name):
            case None =>
                Some(
                  makeExample(exampleSnippet)
                )
            case Some(previous) =>
                Some(previous.merge(makeExample(exampleSnippet)))
        ExampleSnippetMerger(config, updatedExamples)

    private def makeExample(
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): Example =
        val snippet = exampleSnippet.snippet
        Example(
          exampleSnippet.name,
          config
              .templateId(snippet.language, snippet.properties),
          snippet.language,
          List(
            ExampleContent(
              config.prefixTemplateIds(snippet),
              snippet.lines,
              config.suffixTemplateIds(snippet)
            )
          )
        )

private object ExampleSnippetMerger:
    def empty(config: Yadladoc.Configuration) =
        ExampleSnippetMerger(config, Map.empty)
