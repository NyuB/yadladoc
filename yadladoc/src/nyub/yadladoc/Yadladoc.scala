package nyub.yadladoc

import java.nio.file.Path
import java.nio.file.Files
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

class Yadladoc(private val settings: Yadladoc.Settings):
    def run(markdownFile: Path): Unit =
        val snippets = FileIterable(markdownFile)
            .use(Markdown.parse(_))
            .collect:
                case s: Markdown.Snippet => s
            .foldLeft(SnippetMerger(settings, Map.empty))(_.accumulate(_))
            .snippets
        snippets.values.foreach: merged =>
            val snippet = merged.snippets.flatMap(_.lines).mkString("\n")
            val templating =
                TemplateInjection(Map(snippetInjectionKey -> snippet))
            val templated = FileIterable(settings.templateFile)
                .use: lines =>
                    lines.map(templating.inject(_))
                .mkString("\n")
            Files.write(
              merged.filePath,
              templated.getBytes(UTF_8)
            )

    private val snippetInjectionKey = "ydoc.snippet"

object Yadladoc:
    case class Settings(
        val outputDir: Path,
        val templateFile: Path,
        val examplePropertyPrefix: String = "ydoc.example",
        val defaultExampleFileExtension: String = "txt"
    ):
        def extensionForLanguage(language: Option[String]): String =
            language.getOrElse(defaultExampleFileExtension)

        def filePathForExample(
            exampleName: String,
            language: Option[String]
        ): Path =
            outputDir / s"${exampleName}.${extensionForLanguage(language)}"

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

private case class MergedSnippets(
    filePath: Path,
    snippets: List[Markdown.Snippet]
)

private class SnippetMerger(
    val settings: Yadladoc.Settings,
    val snippets: Map[String, MergedSnippets]
):
    def accumulate(snippet: Markdown.Snippet): SnippetMerger =
        val ydocExampleProperty = snippet.header.properties
            .find: s =>
                s.startsWith(s"${settings.examplePropertyPrefix}.") &&
                    !s.substring(settings.examplePropertyPrefix.length + 1)
                        .isBlank
            .map(_.substring(settings.examplePropertyPrefix.length + 1))

        ydocExampleProperty match
            case None => this // no doc should be genrated for this snippet
            case Some(exampleName) =>
                val updatedSnippets = snippets.updatedWith(exampleName):
                    case None =>
                        val filePath = settings.filePathForExample(
                          exampleName,
                          snippet.header.language
                        )
                        Some(MergedSnippets(filePath, List(snippet)))
                    case Some(MergedSnippets(fp, previous)) =>
                        Some(MergedSnippets(fp, previous :+ snippet))
                SnippetMerger(settings, updatedSnippets)
