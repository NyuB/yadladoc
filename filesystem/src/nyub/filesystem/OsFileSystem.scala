package nyub.filesystem

import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

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
        path.writeParents()
        Files.write(path, content.getBytes(charSet)): @annotation.nowarn(
          "msg=discarded non-Unit value"
        )

    override def createTempDirectory(prefix: String): Path =
        Files.createTempDirectory(prefix)

    override def toFileTree(path: Path): Option[FileTree] =
        if path.toFile().isFile() then Some(FileTree.File(path))
        else if path.toFile().isDirectory() then Some(FileTree.Dir(path))
        else None

    override def children(path: Path): Set[Path] =
        Option(path.toFile().list())
            .map(_.toSet.map(Paths.get(_)))
            .getOrElse(Set.empty)

    extension (p: Path)
        private def writeParents(): Unit =
            Option(p.getParent()).foreach: parent =>
                Files.createDirectories(parent)
