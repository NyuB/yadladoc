package nyub.yadladoc

import java.nio.file.Path

sealed trait FileTree:
    val path: Path

object FileTree:
    case class File(override val path: Path) extends FileTree
    case class Dir(override val path: Path) extends FileTree

trait FileSystem:
    def content(path: Path): String
    def useLines[T](path: Path)(f: Iterable[String] => T): T
    def writeContent(path: Path, content: String): Unit
    def createTempDirectory(prefix: String): Path
    extension (p: Path) def toFileTree: FileTree
    extension (p: Path) def children: Set[Path]
