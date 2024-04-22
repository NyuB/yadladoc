package nyub.yadladoc

import java.nio.file.Path

class DirectoryDiffSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:
    private val withTwoDir = FunFixture.map2(withTempDir, withTempDir)
    private val differ = DirectoryDiffer(OsFileSystem())

    withTwoDir.test("Empty folders are the same"): (dirA, dirB) =>
        differ.diff(dirA, dirB) isEqualTo DirectoryDiff.SAME

    withTwoDir.test("Single file in A, nothing in B"): (dirA, dirB) =>
        touch(dirA, "a.txt")
        differ.diff(dirA, dirB) isEqualTo DirectoryDiff(
          Set(p"a.txt"),
          Set.empty,
          Set.empty
        )

    withTwoDir.test("Shared part, diverging branches"): (dirA, dirB) =>
        touch(dirA, "a.txt")
        touch(dirB / "onlyB", "b.txt")
        touch(dirA / "common", "shared.txt")
        touch(dirB / "common", "shared.txt")

        differ.diff(dirA, dirB) isEqualTo DirectoryDiff(
          Set(p"a.txt"),
          Set(p"onlyB/b.txt"),
          Set.empty
        )

    withTwoDir.test("Same filename, different contents"): (dirA, dirB) =>
        makeFile(dirA, "content.txt", "AAA")
        makeFile(dirB, "content.txt", "BBB")

        differ.diff(dirA, dirB) isEqualTo DirectoryDiff(
          Set.empty,
          Set.empty,
          Set(p"content.txt")
        )

    private def touch(dir: Path, filename: String): Unit =
        makeFile(dir, filename, "")

end DirectoryDiffSuite
