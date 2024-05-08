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
                example.language.getOrElse(Yadladoc.DEFAULT_LANGUAGE).name
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
            val prefixLines = c.prefixTemplateNames
                .map(config.templateFile(_))
                .flatMap: templateFile =>
                    fileSystem.useLines(templateFile)(
                      _.map(templating.inject(_, injectionProperties))
                    )
            val suffixLines = c.suffixTemplateNames
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

        def templateFile(templateName: String): Path =
            includesDir / s"${templateName}.template"

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

        def extensionForLanguage(languageOrNone: Option[Language]): String =
            languageOrNone
                .map: lang =>
                    properties.getOrDefault(
                      s"ydoc.language.${lang.name}.extension"
                    )(
                      lang.name
                    )
                .getOrElse("txt")

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
              s"${exampleName}.${extensionForLanguage(exampleLanguage)}"
            )

        def exampleSanitizedName(exampleName: String): String =
            exampleName.replaceAll("[/\\:]+", "_")

        def prefixTemplateNames(
            header: Snippet.Header
        ): Iterable[String] =
            header.properties.get("ydoc.prefix").toList

        def suffixTemplateNames(
            header: Snippet.Header
        ): Iterable[String] =
            header.properties.get("ydoc.suffix").toList

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
          snippet.header.language,
          List(
            ExampleContent(
              config.prefixTemplateNames(snippet.header),
              snippet.lines,
              config.suffixTemplateNames(snippet.header)
            )
          )
        )
