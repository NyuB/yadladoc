package nyub.yadladoc.app

import nyub.assert.AssertExtensions
import java.nio.file.Paths

class CommandsSuite extends munit.FunSuite with AssertExtensions:
    test("help"):
        YdocMain().parse(Seq("help")) `is equal to` Help(false)

    test("help does not expect any more argument"):
        YdocMain().parse(Seq("help", "README.md")) `is equal to` Help(true)

    test("run <file>"):
        YdocMain().parse(Seq("run", "README.md")) `is equal to` Run(
          Seq(Paths.get("README.md"))
        )

    test("run expects at least one path argument"):
        YdocMain().parse(Seq("run")) `is equal to` Help(true)

    test("check <file>"):
        YdocMain().parse(Seq("check", "README.md")) `is equal to` Check(
          Seq(Paths.get("README.md"))
        )

    test("check expects at least one path argument"):
        YdocMain().parse(Seq("check")) `is equal to` Help(true)

end CommandsSuite
