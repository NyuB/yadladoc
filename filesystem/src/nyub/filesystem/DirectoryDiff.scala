package nyub.filesystem

import java.nio.file.Path
import FileTree.{Dir, File}

/** Represents the difference between two directories (denoted `A` & `B`)
  *
  * All paths are expressed relative to their respective diff root, so a path
  * `c/b` in [[#onlyInA]] represents the absolute path `A/b/c`
  * @param onlyInA
  *   the file paths present in directory A and not in B
  * @param onlyInB
  *   the file paths present in directory B and not in A
  * @param different
  *   the files paths presents in both A & B but with different contents
  */
case class DirectoryDiff(
    val onlyInA: Set[Path],
    val onlyInB: Set[Path],
    val different: Set[Path]
)

object DirectoryDiff:
    /** Represents the (absence of) diff between two identical directories
      */
    val SAME = DirectoryDiff(Set.empty, Set.empty, Set.empty)

/** A diffing method that will compare two paths A & B reading from two
  * respective [[FileSystem]]s
  *
  * @param fsa
  *   the filesystem to use to read A files
  * @param fsb
  *   the filesystem to use to read B files
  */
class DirectoryDiffer(private val fsa: FileSystem, private val fsb: FileSystem):
    def this(fs: FileSystem) = this(fs, fs)

    /** Compare structure and file content of two directories and produce the
      * differing file paths as a [[DirectoryDiff]]
      *
      * @param rootA
      *   the first root directory compared to `rootB`
      * @param rootB
      *   the second root directory compared to `rootA`
      * @return
      *   a directory diff with paths expressed relatively to `rootA` and
      *   `rootB`
      */
    def diff(rootA: Path, rootB: Path): DirectoryDiff =
        diffInternal(rootA, rootB).relativize(rootA, rootB)

    private def diffInternal(
        rootA: Path,
        rootB: Path
    ): DirectoryDiff = fsa.toFileTree(rootA) -> fsb.toFileTree(rootB) match
        case (Some(ta), Some(tb)) =>
            ta -> tb match
                case (File(fa), Dir(db)) =>
                    DirectoryDiff(Set(fa), all(db, fsb), Set.empty)

                case (Dir(da), File(fb)) =>
                    DirectoryDiff(all(da, fsa), Set(fb), Set.empty)

                case (File(fa), File(fb)) => fileDiff(fa, fb)
                case (Dir(da), Dir(db))   => dirDiff(da, db)
        case (None, Some(File(fb))) =>
            DirectoryDiff(Set.empty, Set(fb), Set.empty)
        case (Some(File(fa)), None) =>
            DirectoryDiff(Set(fa), Set.empty, Set.empty)
        case (None, Some(Dir(db))) =>
            DirectoryDiff(Set.empty, all(db, fsb), Set.empty)
        case (Some(Dir(da)), None) =>
            DirectoryDiff(all(da, fsa), Set.empty, Set.empty)
        case (None, None) => DirectoryDiff.SAME

    private def dirDiff(da: Path, db: Path): DirectoryDiff =
        val childrenA = fsa.children(da)
        val childrenB = fsb.children(db)
        val both = childrenA
            .intersect(childrenB)
            .map(p => diffInternal(da / p, db / p))
        val onlyA =
            childrenA.removedAll(childrenB).flatMap(p => all(da / p, fsa))
        val onlyB =
            childrenB.removedAll(childrenA).flatMap(p => all(db / p, fsb))
        both.foldLeft(DirectoryDiff(onlyA, onlyB, Set.empty)): (acc, item) =>
            acc.merge(item)

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
            case Some(File(f)) => Set(f)
            case Some(Dir(d)) =>
                fs.children(d).flatMap(child => all(d / child, fs))
            case None => Set.empty

    extension (d: DirectoryDiff)
        private def merge(other: DirectoryDiff) = DirectoryDiff(
          d.onlyInA ++ other.onlyInA,
          d.onlyInB ++ other.onlyInB,
          d.different ++ other.different
        )

        private def relativize(rootA: Path, rootB: Path) = DirectoryDiff(
          d.onlyInA.map(rootA.relativize(_)),
          d.onlyInB.map(rootB.relativize(_)),
          d.different.map(rootA.relativize(_))
        )
