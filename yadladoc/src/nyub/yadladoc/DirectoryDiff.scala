package nyub.yadladoc

import nyub.yadladoc.filesystem.{/, FileSystem}
import nyub.yadladoc.filesystem.FileTree.{Dir, File}

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

class DirectoryDiffer(private val fsa: FileSystem, private val fsb: FileSystem):
    def this(fs: FileSystem) = this(fs, fs)

    def diff(rootA: Path, rootB: Path): DirectoryDiff =
        diffInternal(rootA, rootB).relativize(rootA, rootB)

    private def diffInternal(
        rootA: Path,
        rootB: Path
    ): DirectoryDiff = fsa.toFileTree(rootA) -> fsb.toFileTree(rootB) match
        case (File(fa), File(fb)) => fileDiff(fa, fb)
        case (File(fa), Dir(db)) =>
            DirectoryDiff(Set(fa), all(db, fsb), Set.empty)
        case (Dir(da), File(fb)) =>
            DirectoryDiff(all(da, fsa), Set(fb), Set.empty)
        case (Dir(da), Dir(db)) =>
            val childrenA = fsa.children(da)
            val childrenB = fsb.children(db)
            val both = childrenA
                .intersect(childrenB)
                .map(p => diffInternal(da / p, db / p))
            val onlyA =
                childrenA.removedAll(childrenB).flatMap(p => all(da / p, fsa))
            val onlyB =
                childrenB.removedAll(childrenA).flatMap(p => all(db / p, fsb))
            both.foldLeft(DirectoryDiff(onlyA, onlyB, Set.empty))((acc, item) =>
                acc.merge(item)
            )

    private def fileDiff(
        fa: Path,
        fb: Path
    ): DirectoryDiff =
        if fa.getFileName() == fb.getFileName() then
            if fsa.content(fa) == fsb.content(fb) then DirectoryDiff.SAME
            else DirectoryDiff(Set.empty, Set.empty, Set(fa))
        else DirectoryDiff(Set(fa), Set(fb), Set.empty)

    private def all(root: Path, fs: FileSystem): Set[Path] =
        fs.toFileTree(root) match
            case File(f) => Set(f)
            case Dir(d)  => fs.children(d).flatMap(child => all(d / child, fs))
