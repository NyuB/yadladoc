package nyub.yadladoc

import java.nio.file.Path
import java.nio.file.Files
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source
import nyub.yadladoc.Markdown.Snippet.Header
import nyub.yadladoc.Yadladoc.Examplable

class Yadladoc(private val config: Yadladoc.Configuration):
    def run(markdownFile: Path): Unit =
        val mergedSnippets = FileIterable(markdownFile)
            .use(Markdown.parse(_))
            .collect:
                case s: Markdown.Snippet => s
            .foldLeft(SnippetMerger(config, Map.empty))(_.accumulate(_))
            .snippets

        mergedSnippets.values.foreach: merged =>
            val templating =
                TemplateInjection(
                  Map(config.snippetInjectionKey -> merged.mergedLines)
                )
            val templated =
                FileIterable(config.templateFileForSnippet(merged.sharedHeader))
                    .use: lines =>
                        lines.map(templating.inject(_))
                    .mkString("\n")
            Files.write(
              merged.filePath,
              templated.getBytes(UTF_8)
            )

object Yadladoc:
    trait Configuration:
        def exampleForSnippet(header: Markdown.Snippet.Header): Examplable
        def templateFileForSnippet(header: Markdown.Snippet.Header): Path
        def snippetInjectionKey: String

    enum Examplable:
        case Example(val path: Path, val name: String)
        case Ignore

    case class Settings(
        val outputDir: Path,
        val configDir: Path,
        val examplePropertyPrefix: String = "ydoc.example",
        val defaultExampleFileExtension: String = "txt",
        override val snippetInjectionKey: String = "ydoc.snippet"
    ) extends Configuration:
        override def exampleForSnippet(header: Header): Examplable =
            val exampleNameProperty = header.properties.find: s =>
                if !s.startsWith(examplePropertyPrefix + ".") then false
                else
                    val name = s.substring(examplePropertyPrefix.length + 1)
                    !name.isBlank
            exampleNameProperty
                .map: s =>
                    val name = s.substring(examplePropertyPrefix.length + 1)
                    Examplable.Example(
                      outputDir / s"${name}.${extensionForLanguage(header.language)}",
                      name
                    )
                .getOrElse(Examplable.Ignore)

        override def templateFileForSnippet(header: Header): Path =
            header.language
                .map: lang =>
                    configDir / s"${lang}.template"
                .getOrElse(configDir / "default.template")

        private def extensionForLanguage(lang: Option[String]): String =
            lang.getOrElse(defaultExampleFileExtension)

extension (p: Path)
    private def /(other: Path) = p.resolve(other)
    private def /(other: String) = p.resolve(other)

private class FileIterable(path: Path):
    def use[T](f: Iterable[String] => T): T =
        val linesSource = Source.fromFile(path.toFile())
        val iterable: Iterable[String] = new:
            override def iterator: Iterator[String] = linesSource.getLines()

        val res = f(iterable)
        linesSource.close()
        res

private case class MergedSnippets private (
    val filePath: Path,
    val sharedHeader: Markdown.Snippet.Header,
    snippets: List[Markdown.Snippet]
):
    def merge(snippet: Markdown.Snippet): MergedSnippets =
        val snippetLanguage = snippet.header.language
        val sharedLanguage = sharedHeader.language
        if snippetLanguage != sharedLanguage then
            throw IllegalArgumentException(
              s"Error trying to merge snippets with different languages ${sharedLanguage} and ${snippetLanguage}"
            )
        else MergedSnippets(filePath, sharedHeader, snippets :+ snippet)

    def mergedLines: String = snippets.flatMap(_.lines).mkString("\n")

private object MergedSnippets:
    def init(filePath: Path, snippet: Markdown.Snippet): MergedSnippets =
        MergedSnippets(filePath, snippet.header, List(snippet))

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
