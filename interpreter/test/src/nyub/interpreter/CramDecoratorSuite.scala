package nyub.interpreter

import nyub.assert.AssertExtensions
import java.nio.file.Path
import java.nio.file.Paths

class CramDecoratorSuite extends munit.FunSuite with AssertExtensions:
    test("Nominal test"):
        val cramDecorator = CramDecorator(bashPath())
        cramDecorator.decorate(
          Seq(
            "Juste a comment",
            "  $ echo a",
            "  $ echo aaa > a.txt",
            "  $ cat a.txt",
            "  this line will be replaced by the actual output of the above command",
            "  $ cat a"
          )
        ) `is equal to`
            Seq(
              "Juste a comment",
              "  $ echo a",
              "  a",
              "  $ echo aaa > a.txt",
              "  $ cat a.txt",
              "  aaa",
              "  $ cat a",
              "  cat: a: No such file or directory",
              "  [1]"
            )

    private def bashPath(): Path =
        val osDependent =
            if System.getProperty("os.name").toLowerCase().contains("windows")
            then "C:\\Windows\\System32\\bash.exe"
            else "/bin/bash"
        Paths.get(osDependent)

end CramDecoratorSuite
