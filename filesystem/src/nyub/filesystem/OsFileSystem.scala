package nyub.filesystem

import java.nio.file.{Files, Path}
import java.nio.charset.{Charset, StandardCharsets}
import scala.io.Source
import java.nio.file.Paths

/** Implements [[FileSystem]] with usual access to disk
  */
class OsFileSystem(private val charSet: Charset = StandardCharsets.UTF_8)
    extends FileSystem:
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
        Files.createDirectories(path.getParent())
        Files.write(path, content.getBytes(charSet)): @annotation.nowarn(
          "msg=discarded non-Unit value"
        )

    override def createTempDirectory(prefix: String): Path =
        Files.createTempDirectory(prefix)

    extension (p: Path)
        override def toFileTree: Option[FileTree] =
            if p.toFile().isFile() then Some(FileTree.File(p))
            else if p.toFile().isDirectory() then Some(FileTree.Dir(p))
            else None

    extension (p: Path)
        override def children: Set[Path] =
            Option(p.toFile().list())
                .map(_.toSet.map(Paths.get(_)))
                .getOrElse(Set.empty)
