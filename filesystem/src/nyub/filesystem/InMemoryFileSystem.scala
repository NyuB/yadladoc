package nyub.filesystem

import java.nio.file.Path
import java.nio.file.Paths
import nyub.filesystem.Tree.Node
import nyub.filesystem.Tree.Leaf

/** Simulate a file system without actually reading from or writing to disk
  */
class InMemoryFileSystem private (private var root: Node) extends FileSystem:
    override def content(path: Path): String = root.content(path)

    override def createTempDirectory(prefix: String): Path =
        var id = prefix
        var index = 0
        while root.children.contains(prefix) do
            id = s"${id}_${index}"
            index = 1
        val res = Paths.get(s"/${id}")
        root = root.insert(res, Node(Map.empty))
        res

    override def useLines[T](path: Path)(f: Iterable[String] => T): T =
        val lines = content(path).split("(\\r\\n)|(\\n)")
        f(lines)

    override def writeContent(path: Path, content: String): Unit =
        root = root.insert(path, Tree.Leaf(content))

    override def children(path: Path): Set[Path] = root.get(path) match
        case Some(Node(children)) => children.keySet.map(Paths.get(_))
        case _ =>
            throw IllegalArgumentException(s"${path} is not a directory")

    override def toFileTree(path: Path): Option[FileTree] = root
        .get(path)
        .map:
            case Node(_) => FileTree.Dir(path)
            case Leaf(_) => FileTree.File(path)

object InMemoryFileSystem:
    /** Generate an empty file system simulation
      */
    def init() = InMemoryFileSystem(Node(Map.empty))

sealed private trait Tree:
    def content(path: Path): String
    def get(path: Path): Option[Tree]

private object Tree:
    case class Node(children: Map[String, Tree]) extends Tree:
        override def content(path: Path): String =
            if path.isEmpty then
                throw IllegalArgumentException(s"${this} is not a file")
            else
                val (first, sub) = path.headTail
                children.get(first) match
                    case None =>
                        throw IllegalArgumentException(
                          s"${first} is not in ${this}"
                        )
                    case Some(tree) =>
                        tree.content(sub)

        override def get(path: Path): Option[Tree] =
            if path.isEmpty then Some(this)
            else
                val (first, sub) = path.headTail
                children.get(first).flatMap(_.get(sub))

        def insert(path: Path, tree: Tree): Node =
            if path.isEmpty then
                throw IllegalArgumentException("Cannot insert at root")
            else
                val (first, sub) = path.headTail
                if path.getNameCount() == 1 then
                    Node(children.updated(first, tree))
                else
                    val child: Node = children.get(first) match
                        case None          => Node(Map.empty).insert(sub, tree)
                        case Some(Leaf(_)) => Node(Map.empty).insert(sub, tree)
                        case Some(d: Node) => d.insert(sub, tree)
                    Node(children.updated(first, child))

    case class Leaf(val content: String) extends Tree:
        override def content(path: Path) = content
        override def get(path: Path): Option[Tree] =
            if !path.isEmpty then None
            else Some(this)

    extension (p: Path)
        private def headTail: (String, Path) =
            val sub =
                if p.isEmpty then
                    throw IllegalArgumentException("Empty path has no head")
                else if p.getNameCount() == 1 then Paths.get("/")
                else p.subpath(1, p.getNameCount())
            p.getName(0).toString() -> sub

        private def isEmpty: Boolean = p.getNameCount() == 0
