package nyub.filesystem

import nyub.assert.AssertExtensions
import java.nio.file.Files

class PathExtensionsSuite extends munit.FunSuite with AssertExtensions:
    test("Read lines from path"):
        val tmp = Files.createTempDirectory("testLines") / "content.txt"
        Files.writeString(tmp, "First line\nSecond line")
        tmp.lines `is equal to` Iterable("First line", "Second line")

    test("Return an empty string as last line for trailing newline"):
        val tmp = Files.createTempDirectory("testLines") / "content.txt"
        Files.writeString(tmp, "First line\n")
        tmp.lines `is equal to` Iterable("First line", "")

    test("Handle \\r\\n as single newline"):
        val tmp = Files.createTempDirectory("testLines") / "content.txt"
        Files.writeString(tmp, "A\r\nB")
        tmp.lines `is equal to` Iterable("A", "B")
