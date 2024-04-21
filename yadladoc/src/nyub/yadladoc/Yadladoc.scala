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
            val templating =
                TemplateInjection(
                  Map(config.snippetInjectionKey -> example.body.mkString("\n"))
                )
            val templated =
                storageAccess.useLines(
                  config.templateFile(
                    example.language.getOrElse("default")
                  )
                ): lines =>
                    lines.map(templating.inject(_)).mkString("\n")
            storageAccess.writeContent(
              outputDir / config.exampleFile(example.name, example.language),
              templated
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

private case class Example private (
    val name: String,
    val language: Option[String],
    prefixTemplates: Iterable[Path],
    body: Iterable[String],
    suffixTemplates: Iterable[Path]
):
    def merge(snippet: Markdown.Snippet): Example =
        val snippetLanguage = snippet.header.language
        if snippetLanguage != language then
            throw IllegalArgumentException(
              s"Error trying to merge snippets with different languages ${language} and ${snippetLanguage}"
            )
        else
            Example(
              name,
              language,
              prefixTemplates,
              body ++ snippet.lines,
              suffixTemplates
            )

private object Example:
    def init(
        name: String,
        language: Option[String],
        lines: Iterable[String]
    ): Example =
        Example(
          name,
          language,
          List.empty,
          lines,
          List.empty
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
                          Example.init(
                            exampleName,
                            snippet.header.language,
                            snippet.lines
                          )
                        )
                    case Some(previous) =>
                        Some(previous.merge(snippet))
                SnippetMerger(config, updatedSnippets)
