package nyub.yadladoc

import nyub.markdown.Markdown
import nyub.yadladoc.Yadladoc.Examplable
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
            .flatMap: f =>
                fileSystem.toFileTree(outputDir / f.file) -> checkFs.toFileTree(
                  checkDir / f.file
                ) match
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
                            case None => List(CheckErrors.MissingFile(f.file))
                            case Some(FileTree.Dir(_)) =>
                                List(CheckErrors.MissingFile(f.file))
                            case Some(FileTree.File(af)) =>
                                val actualContent = fileSystem.content(af)
                                val expectedContent =
                                    checkFs.content(expected)
                                if actualContent == expectedContent then
                                    List.empty
                                else
                                    List(
                                      CheckErrors.MismatchingContent(
                                        f.file,
                                        actualContent,
                                        expectedContent
                                      )
                                    )
            .toList

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Iterable[GeneratedFile] =
        val examples = snippets(markdownFile)
            .foldLeft(SnippetMerger.empty(config))(_.accumulate(_))
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

        def exampleForSnippet(snippet: Snippet): Examplable =
            snippet.properties
                .get(exampleNamePropertyKey)
                .filterNot(_.isBlank)
                .map(Examplable.MakeExample(_))
                .getOrElse(Examplable.Ignore)

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

    enum Examplable:
        case MakeExample(val name: String)
        case Ignore

    case class ConfigurationFromFile(
        override val configDir: Path,
        private val storage: FileSystem = OsFileSystem()
    ) extends Configuration:
        override val properties: Properties =
            val propertyFile = configDir / "ydoc.properties"
            if !propertyFile.toFile().isFile() then Properties.empty
            else
                storage.useLines(configDir / "ydoc.properties"): lines =>
                    lines.foldLeft(Properties.empty): (props, line) =>
                        props.extendedWith(Properties.ofLine(line))

private class SnippetMerger(
    val config: Yadladoc.Configuration,
    val examples: Map[String, Example]
):
    def accumulate(snippet: Snippet): SnippetMerger =
        val ydocExample = config.exampleForSnippet(snippet)

        ydocExample match
            case Examplable.Ignore =>
                this // no doc should be generated for this snippet
            case Examplable.MakeExample(exampleName) =>
                val updatedSnippets = examples.updatedWith(exampleName):
                    case None =>
                        Some(
                          makeExample(exampleName, snippet)
                        )
                    case Some(previous) =>
                        Some(previous.merge(makeExample(exampleName, snippet)))
                SnippetMerger(config, updatedSnippets)

    private def makeExample(name: String, snippet: Snippet): Example =
        Example(
          name,
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

private object SnippetMerger:
    def empty(config: Yadladoc.Configuration) = SnippetMerger(config, Map.empty)
