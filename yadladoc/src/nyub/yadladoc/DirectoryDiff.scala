package nyub.yadladoc

import java.nio.file.Path

case class DirectoryDiff(
    val onlyInA: List[Path],
    val onlyInB: List[Path],
    val different: List[Path]
)

object DirectoryDiff:
    val SAME = DirectoryDiff(List.empty, List.empty, List.empty)
    def diff(rootA: Path, rootB: Path): DirectoryDiff = DirectoryDiff.SAME
