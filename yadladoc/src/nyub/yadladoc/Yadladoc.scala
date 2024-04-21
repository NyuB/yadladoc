package nyub.yadladoc

import java.nio.file.{Files, Path, Paths}
import nyub.yadladoc.Markdown.Snippet.Header
import nyub.yadladoc.Yadladoc.Examplable

class Yadladoc(
    private val config: Yadladoc.Configuration,
    private val storageAccess: StorageAccess = FilesAccess()
):
    def run(outputDir: Path, markdownFile: Path): Unit =
        val mergedSnippets = FileIterable(markdownFile)
            .use(Markdown.parse(_))
            .collect:
                case s: Markdown.Snippet => s
            .foldLeft(SnippetMerger(config, Map.empty))(_.accumulate(_))
            .snippets

        mergedSnippets.values.foreach: merged =>
            val templating =
                TemplateInjection(
                  Map(config.snippetInjectionKey -> merged.body.mkString("\n"))
                )
            val templated =
                storageAccess.useLines(
                  config.templateFileForSnippet(
                    merged.sharedHeader
                  )
                ): lines =>
                    lines.map(templating.inject(_)).mkString("\n")
            storageAccess.writeContent(
              outputDir / merged.filePath,
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
        def templateFileForSnippet(header: Markdown.Snippet.Header): Path
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
                .map: name =>
                    Examplable.Example(
                      Paths.get(
                        s"${name}.${extensionForLanguage(header.language)}"
                      ),
                      name
                    )
                .getOrElse(Examplable.Ignore)

    enum Examplable:
        case Example(val path: Path, val name: String)
        case Ignore

    case class ConfigurationFromFile(
        val configDir: Path,
        private val storage: StorageAccess = FilesAccess()
    ) extends Configuration:
        override val properties: Properties =
            val propertyFile = configDir / "ydoc.properties"
            if !propertyFile.toFile().isFile() then Properties.empty
            else
                storage.useLines(configDir / "ydoc.properties"): lines =>
                    lines.foldLeft(Properties.empty): (props, line) =>
                        props.extendedWith(Properties.ofLine(line))

        override def templateFileForSnippet(header: Header): Path =
            header.language
                .map: lang =>
                    configDir / s"${lang}.template"
                .getOrElse(configDir / "default.template")

private case class MergedSnippets private (
    val filePath: Path,
    val sharedHeader: Markdown.Snippet.Header,
    prefixTemplates: Iterable[Path],
    body: Iterable[String],
    suffixTemplates: Iterable[Path]
):
    def merge(snippet: Markdown.Snippet): MergedSnippets =
        val snippetLanguage = snippet.header.language
        val sharedLanguage = sharedHeader.language
        if snippetLanguage != sharedLanguage then
            throw IllegalArgumentException(
              s"Error trying to merge snippets with different languages ${sharedLanguage} and ${snippetLanguage}"
            )
        else
            MergedSnippets(
              filePath,
              sharedHeader,
              prefixTemplates,
              body ++ snippet.lines,
              suffixTemplates
            )

private object MergedSnippets:
    def init(filePath: Path, snippet: Markdown.Snippet): MergedSnippets =
        MergedSnippets(
          filePath,
          snippet.header,
          List.empty,
          snippet.lines,
          List.empty
        )

private class SnippetMerger(
    val config: Yadladoc.Configuration,
    val snippets: Map[String, MergedSnippets]
):
    def accumulate(snippet: Markdown.Snippet): SnippetMerger =
        val ydocExample = config.exampleForSnippet(snippet.header)

        ydocExample match
            case Examplable.Ignore =>
                this // no doc should be generated for this snippet
            case Examplable.Example(examplePath, exampleName) =>
                val updatedSnippets = snippets.updatedWith(exampleName):
                    case None =>
                        Some(MergedSnippets.init(examplePath, snippet))
                    case Some(previous) =>
                        Some(previous.merge(snippet))
                SnippetMerger(config, updatedSnippets)
