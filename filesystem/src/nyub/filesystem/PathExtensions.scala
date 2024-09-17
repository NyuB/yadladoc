package nyub.filesystem

import java.nio.file.Path
import scala.io.Source

extension (p: Path)
    def /(other: Path) = p.resolve(other)
    def /(other: String) = p.resolve(other)
    def useLines[T](f: Iterable[String] => T): T = FileIterable(p).use(f)

private class FileIterable(path: Path):
    def use[T](f: Iterable[String] => T): T =
        val linesSource = Source.fromFile(path.toFile())
        val iterable: Iterable[String] = new:
            override def iterator: Iterator[String] = linesSource.getLines()

        val res = f(iterable)
        linesSource.close()
        res
