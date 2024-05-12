package nyub.yadladoc

import nyub.yadladoc.Markdown.Snippet
import nyub.yadladoc.Yadladoc.Examplable
import nyub.filesystem.{
    /,
    useLines,
    DirectoryDiff,
    DirectoryDiffer,
    FileSystem,
    InMemoryFileSystem,
    OsFileSystem
}
import nyub.yadladoc.templating.{
    SurroundingTemplateInjection,
    TemplateInjection
}

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

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Iterable[GeneratedFile] =
        val examples = markdownFile
            .useLines(Markdown.parse(_))
            .collect:
                case s: Snippet => s
            .foldLeft(SnippetMerger(config, Map.empty))(_.accumulate(_))
            .examples

        examples.values.map: example =>
            val fullExample = buildExample(example)
            val finalTemplatingProperties =
                templatingProperties(example, fullExample)
            val finalTemplate = fileSystem.useLines(
              config.templateFile(
                example.template
              )
            )(_.map(templating.inject(_, finalTemplatingProperties)))

            val exampleFile = config.exampleFile(example.name, example.language)
            writeFs.writeContent(
              outputDir / exampleFile,
              finalTemplate.mkString("\n")
            )
            GeneratedFile(exampleFile, markdownFile)

    def check(outputDir: Path, markdownFile: Path): List[Errors] =
        val checkFs = InMemoryFileSystem.init()
        val checkDir = checkFs.createTempDirectory("check")
        run(checkDir, markdownFile, checkFs)
        val diff =
            DirectoryDiffer(checkFs, fileSystem).diff(checkDir, outputDir)
        diff.onlyInA.toList.map(
          CheckErrors.MissingFile(_)
        ) ++ diff.different.toList.map(f =>
            CheckErrors.MismatchingContent(
              f,
              fileSystem.content(outputDir / f),
              checkFs.content(checkDir / f)
            )
        )

    private def buildExample(
        example: Example
    ): Iterable[String] =
        example.content.zipWithIndex.flatMap: (c, i) =>
            val injectionProperties = templatingProperties(example, i)
            val prefixLines = c.prefixTemplateIds
                .map(config.templateFile(_))
                .flatMap: templateFile =>
                    fileSystem.useLines(templateFile)(
                      _.map(templating.inject(_, injectionProperties))
                    )
            val suffixLines = c.suffixTemplateIds
                .map(config.templateFile(_))
                .flatMap: templateFile =>
                    fileSystem.useLines(templateFile)(
                      _.map(templating.inject(_, injectionProperties))
                    )
            prefixLines ++ c.body ++ suffixLines

    private def templatingProperties(example: Example): Map[String, String] =
        config.properties.toMap ++ Map(
          config.exampleNameInjectionKey -> config.exampleSanitizedName(
            example.name
          )
        )

    private def templatingProperties(
        example: Example,
        index: Int
    ): Map[String, String] =
        templatingProperties(example) ++ Map(
          config.subExampleNameInjectionKey -> config.exampleSanitizedName(
            s"${example.name}_${index}"
          )
        )

    private def templatingProperties(
        example: Example,
        content: Iterable[String]
    ): Map[String, String] =
        templatingProperties(example) ++ Map(
          config.snippetInjectionKey -> content.mkString("\n")
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

        def exampleForSnippet(header: Snippet.Header): Examplable =
            header.properties
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

        def exampleSanitizedName(exampleName: String): String =
            exampleName.replaceAll("[/\\:.]+", "_")

        def prefixTemplateIds(
            header: Snippet.Header
        ): Iterable[TemplateId] =
            header.properties.get("ydoc.prefix").toList.map(TemplateId(_))

        def suffixTemplateIds(
            header: Snippet.Header
        ): Iterable[TemplateId] =
            header.properties.get("ydoc.suffix").toList.map(TemplateId(_))

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
        val ydocExample = config.exampleForSnippet(snippet.header)

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
              .templateId(snippet.header.language, snippet.header.properties),
          snippet.header.language,
          List(
            ExampleContent(
              config.prefixTemplateIds(snippet.header),
              snippet.lines,
              config.suffixTemplateIds(snippet.header)
            )
          )
        )
