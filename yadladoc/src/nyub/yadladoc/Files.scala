package nyub.yadladoc

import java.nio.file.{Files, Path}
import java.nio.charset.{Charset, StandardCharsets}
import scala.io.Source

private class FilesAccess(private val charSet: Charset = StandardCharsets.UTF_8)
    extends StorageAccess:
    override def content(path: Path): String =
        Files.readString(path, charSet)

    override def useLines[T](path: Path)(f: Iterable[String] => T): T =
        val linesSource = Source.fromFile(path.toFile())
        val iterable: Iterable[String] = new:
            override def iterator: Iterator[String] = linesSource.getLines()

        val res = f(iterable)
        linesSource.close()
        res

    override def writeContent(path: Path, content: String): Unit =
        Files.write(path, content.getBytes(charSet))

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
