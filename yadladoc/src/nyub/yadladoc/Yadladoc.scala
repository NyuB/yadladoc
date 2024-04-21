package nyub.yadladoc

import java.nio.file.{Path, Paths}
import nyub.yadladoc.Markdown.Snippet.Header
import nyub.yadladoc.Yadladoc.Examplable

class Yadladoc(
    private val config: Yadladoc.Configuration,
    private val storageAccess: StorageAccess = FilesAccess()
):
    def run(outputDir: Path, markdownFile: Path): Unit =
        val examples = FileIterable(markdownFile)
            .use(Markdown.parse(_))
            .collect:
                case s: Markdown.Snippet => s
            .foldLeft(SnippetMerger(config, Map.empty))(_.accumulate(_))
            .examples

        examples.values.foreach: example =>
            val fullExample = buildExample(
              example,
              TemplateInjection(config.properties.all.toMap)
            ).mkString("\n")
            val finalTemplating = TemplateInjection(
              config.properties.all.toMap ++ Map(
                config.snippetInjectionKey -> fullExample
              )
            )
            val finalTemplate = storageAccess.useLines(
              config.templateFile(example.language.getOrElse("default"))
            )(_.map(finalTemplating.inject(_)))
            storageAccess.writeContent(
              outputDir / config.exampleFile(example.name, example.language),
              finalTemplate.mkString("\n")
            )

    def check(outputDir: Path, markdownFile: Path): List[Errors] =
        val tempDir = storageAccess.createTempDirectory("check")
        run(tempDir, markdownFile)
        val diff =
            DirectoryDiffer(storageAccess).diff(tempDir, outputDir)
        diff.onlyInA.toList.map(
          CheckErrors.MissingFile(_)
        ) ++ diff.onlyInB.toList.map(
          CheckErrors.UnexpectedFile(_)
        ) ++ diff.different.toList.map(f =>
            CheckErrors.MismatchingContent(
              f,
              storageAccess.content(outputDir / f),
              storageAccess.content(tempDir / f)
            )
        )

    private def buildExample(
        example: Example,
        templating: TemplateInjection
    ): Iterable[String] =
        example.content.flatMap: c =>
            val prefixLines = c.prefixTemplateNames
                .map(config.templateFile(_))
                .flatMap: templateFile =>
                    storageAccess.useLines(templateFile)(
                      _.map(templating.inject(_))
                    )
            val suffixLines = c.suffixTemplateNames
                .map(config.templateFile(_))
                .flatMap: templateFile =>
                    storageAccess.useLines(templateFile)(
                      _.map(templating.inject(_))
                    )
            prefixLines ++ c.body ++ suffixLines

object Yadladoc:
    trait Configuration:
        def properties: Properties
        def configDir: Path
        def includesDir: Path = properties.getPathOrDefault("ydoc.includesDir")(
          configDir / "includes"
        )

        def templateFile(templateName: String): Path =
            includesDir / s"${templateName}.template"

        def snippetInjectionKey: String =
            properties.getOrDefault("ydoc.snippetInjectionKey")("ydoc.snippet")

        def exampleNamePropertyKey: String = properties.getOrDefault(
          "ydoc.exampleNamePropertyKey"
        )("ydoc.example")

        def extensionForLanguage(languageOrNone: Option[String]): String =
            languageOrNone
                .map: lang =>
                    properties.getOrDefault(s"ydoc.language.${lang}.extension")(
                      lang
                    )
                .getOrElse(".txt")

        def exampleForSnippet(header: Markdown.Snippet.Header): Examplable =
            header.properties
                .get(exampleNamePropertyKey)
                .filterNot(_.isBlank)
                .map(Examplable.MakeExample(_))
                .getOrElse(Examplable.Ignore)

        def exampleFile(
            exampleName: String,
            exampleLanguage: Option[String]
        ): Path =
            Paths.get(
              s"${exampleName}.${extensionForLanguage(exampleLanguage)}"
            )

        def prefixTemplateNames(
            header: Markdown.Snippet.Header
        ): Iterable[String] =
            header.properties.get("ydoc.prefix").toList

        def suffixTemplateNames(
            header: Markdown.Snippet.Header
        ): Iterable[String] =
            header.properties.get("ydoc.suffix").toList

    enum Examplable:
        case MakeExample(val name: String)
        case Ignore

    case class ConfigurationFromFile(
        override val configDir: Path,
        private val storage: StorageAccess = FilesAccess()
    ) extends Configuration:
        override val properties: Properties =
            val propertyFile = configDir / "ydoc.properties"
            if !propertyFile.toFile().isFile() then Properties.empty
            else
                storage.useLines(configDir / "ydoc.properties"): lines =>
                    lines.foldLeft(Properties.empty): (props, line) =>
                        props.extendedWith(Properties.ofLine(line))

private case class Example(
    val name: String,
    val language: Option[String],
    val content: Iterable[ExampleContent]
):
    def merge(other: Example): Example =
        if other.language != language then
            throw IllegalArgumentException(
              s"Error trying to merge snippets with different languages ${language} and ${other.language}"
            )
        else
            Example(
              name,
              language,
              content ++ other.content
            )

private case class ExampleContent(
    prefixTemplateNames: Iterable[String],
    body: Iterable[String],
    suffixTemplateNames: Iterable[String]
)

private class SnippetMerger(
    val config: Yadladoc.Configuration,
    val examples: Map[String, Example]
):
    def accumulate(snippet: Markdown.Snippet): SnippetMerger =
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

    private def makeExample(name: String, snippet: Markdown.Snippet): Example =
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
