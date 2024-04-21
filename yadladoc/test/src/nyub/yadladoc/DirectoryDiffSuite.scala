package nyub.yadladoc

class DirectoryDiffSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:
    val withTwoDir = FunFixture.map2(withTempDir, withTempDir)

    withTwoDir.test("Empty folders are the same"): (dirA, dirB) =>
        DirectoryDiff.diff(dirA, dirB) isEqualTo DirectoryDiff.SAME

end DirectoryDiffSuite
