package nyub.yadladoc

import java.nio.file.Path

case class DirectoryDiff(
    val onlyInA: Set[Path],
    val onlyInB: Set[Path],
    val different: Set[Path]
):
    def merge(other: DirectoryDiff) = DirectoryDiff(
      onlyInA ++ other.onlyInA,
      onlyInB ++ other.onlyInB,
      different ++ other.different
    )

    def relativize(rootA: Path, rootB: Path) = DirectoryDiff(
      onlyInA.map(rootA.relativize(_)),
      onlyInB.map(rootB.relativize(_)),
      different.map(rootA.relativize(_))
    )

object DirectoryDiff:
    val SAME = DirectoryDiff(Set.empty, Set.empty, Set.empty)

class DirectoryDiffer(private val fs: FileSystem):
    def diff(rootA: Path, rootB: Path): DirectoryDiff =
        diffInternal(rootA, rootB).relativize(rootA, rootB)

    private def diffInternal(
        rootA: Path,
        rootB: Path
    ): DirectoryDiff =
        if rootA.toFile().isFile() && rootB.toFile().isFile()
        then fileDiff(rootA, rootB)
        else if rootA.toFile().isFile() && rootB.toFile().isDirectory() then
            DirectoryDiff(Set(rootA), all(rootB), Set.empty)
        else if rootB.toFile().isFile() && rootA.toFile().isDirectory() then
            DirectoryDiff(all(rootA), Set(rootB), Set.empty)
        else
            val childrenA = rootA.toFile().list().toSet
            val childrenB = rootB.toFile().list().toSet
            val both = childrenA
                .intersect(childrenB)
                .map(p => diffInternal(rootA / p, rootB / p))
            val onlyA =
                childrenA.removedAll(childrenB).flatMap(p => all(rootA / p))
            val onlyB =
                childrenB.removedAll(childrenA).flatMap(p => all(rootB / p))
            both.foldLeft(DirectoryDiff(onlyA, onlyB, Set.empty))((acc, item) =>
                acc.merge(item)
            )

    private def fileDiff(
        rootA: Path,
        rootB: Path
    ): DirectoryDiff =
        if rootA.getFileName() == rootB.getFileName() then
            if fs.content(rootA) == fs.content(rootB) then DirectoryDiff.SAME
            else DirectoryDiff(Set.empty, Set.empty, Set(rootA))
        else DirectoryDiff(Set(rootA), Set(rootB), Set.empty)

    private def all(root: Path): Set[Path] =
        if root.toFile().isFile() then Set(root)
        else
            val children = root.toFile().list()
            if children == null then
                println(s"$root is not a directory")
                Set.empty
            else children.flatMap(child => all(root / child)).toSet
