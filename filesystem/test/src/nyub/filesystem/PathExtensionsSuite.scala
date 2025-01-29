package nyub.filesystem

import nyub.assert.AssertExtensions
import java.nio.file.Files

class PathExtensionsSuite extends munit.FunSuite with AssertExtensions:
    test("Read lines from path"):
        val tmp = Files.createTempDirectory("testLines") / "content.txt"
        Files.writeString(tmp, "First line\nSecond line")
        tmp.lines `is equal to` List("First line", "Second line")
