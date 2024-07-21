package nyub.filesystem

import java.nio.file.{Path, Paths}
import nyub.assert.AssertExtensions

class DirectoryDiffSuite extends munit.FunSuite with AssertExtensions:

    test("Empty folders are the same"):
        given fs: FileSystem = InMemoryFileSystem.init()
        val dirA = fs.createTempDirectory("A")
        val dirB = fs.createTempDirectory("B")

        differ.diff(dirA, dirB) `is equal to` DirectoryDiff.SAME

    test("Single file in A, nothing in B"):
        given fs: FileSystem = InMemoryFileSystem.init()
        val dirA = fs.createTempDirectory("A")
        val dirB = fs.createTempDirectory("B")

        touch(dirA, "a.txt")

        differ.diff(dirA, dirB) `is equal to` DirectoryDiff(
          Set(p"a.txt"),
          Set.empty,
          Set.empty
        )

    test("Shared part, diverging branches"):
        given fs: FileSystem = InMemoryFileSystem.init()
        val dirA = fs.createTempDirectory("A")
        val dirB = fs.createTempDirectory("B")

        touch(dirA, "a.txt")
        touch(dirB / "onlyB", "b.txt")
        touch(dirA / "common", "shared.txt")
        touch(dirB / "common", "shared.txt")

        differ.diff(dirA, dirB) `is equal to` DirectoryDiff(
          Set(p"a.txt"),
          Set(p"onlyB/b.txt"),
          Set.empty
        )

    test("Same filename, different contents"):
        given fs: FileSystem = InMemoryFileSystem.init()
        val dirA = fs.createTempDirectory("A")
        val dirB = fs.createTempDirectory("B")

        writeFile(dirA, "content.txt", "AAA")
        writeFile(dirB, "content.txt", "BBB")

        differ.diff(dirA, dirB) `is equal to` DirectoryDiff(
          Set.empty,
          Set.empty,
          Set(p"content.txt")
        )

    private def differ(using fs: FileSystem) = DirectoryDiffer(fs)
    private def touch(using FileSystem)(dir: Path, filename: String): Unit =
        writeFile(dir, filename, "")

    private def writeFile(using
        fs: FileSystem
    )(dir: Path, filename: String, content: String) =
        fs.writeContent(dir.resolve(filename), content)

    extension (sc: StringContext)
        private def p(args: Any*): Path = Paths.get(sc.s(args*))

end DirectoryDiffSuite
