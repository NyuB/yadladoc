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
    ): DirectoryDiff = fs.toFileTree(rootA) -> fs.toFileTree(rootB) match
        case (FileTree.File(fa), FileTree.File(fb)) => fileDiff(fa, fb)
        case (FileTree.File(fa), FileTree.Dir(db)) =>
            DirectoryDiff(Set(fa), all(db), Set.empty)
        case (FileTree.Dir(da), FileTree.File(fb)) =>
            DirectoryDiff(all(da), Set(fb), Set.empty)
        case (FileTree.Dir(da), FileTree.Dir(db)) =>
            val childrenA = fs.children(da)
            val childrenB = fs.children(db)
            val both = childrenA
                .intersect(childrenB)
                .map(p => diffInternal(da / p, db / p))
            val onlyA =
                childrenA.removedAll(childrenB).flatMap(p => all(da / p))
            val onlyB =
                childrenB.removedAll(childrenA).flatMap(p => all(db / p))
            both.foldLeft(DirectoryDiff(onlyA, onlyB, Set.empty))((acc, item) =>
                acc.merge(item)
            )

    private def fileDiff(
        fa: Path,
        fb: Path
    ): DirectoryDiff =
        if fa.getFileName() == fb.getFileName() then
            if fs.content(fa) == fs.content(fb) then DirectoryDiff.SAME
            else DirectoryDiff(Set.empty, Set.empty, Set(fa))
        else DirectoryDiff(Set(fa), Set(fb), Set.empty)

    private def all(root: Path): Set[Path] = fs.toFileTree(root) match
        case FileTree.File(f) => Set(f)
        case FileTree.Dir(d)  => fs.children(d).flatMap(child => all(d / child))
