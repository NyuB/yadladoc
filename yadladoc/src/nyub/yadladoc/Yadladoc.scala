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
            .flatMap(_.lines)
            .mkString("\n")
        val templating = TemplateInjection(Map("ydoc.snippet" -> snippets))
        val templated = FileIterable(settings.templateFile)
            .use: lines =>
                lines.map(templating.inject(_))
            .mkString("\n")
        Files.write(
          settings.outputDir / outputFilename,
          templated.getBytes(UTF_8)
        )

    private val outputFilename = "yadladoc.txt"

object Yadladoc:
    case class Settings(val outputDir: Path, val templateFile: Path)

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
