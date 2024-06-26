package nyub.filesystem

import java.nio.file.Path

/** Thin abstraction over file access
  */
trait FileSystem:
    def content(path: Path): String
    def useLines[T](path: Path)(f: Iterable[String] => T): T
    def writeContent(path: Path, content: String): Unit
    def createTempDirectory(prefix: String): Path
    def toFileTree(p: Path): Option[FileTree]
    def children(p: Path): Set[Path]

sealed trait FileTree:
    val path: Path

object FileTree:
    case class File(override val path: Path) extends FileTree
    case class Dir(override val path: Path) extends FileTree
