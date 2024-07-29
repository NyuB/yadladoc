package nyub.interpreter

import nyub.assert.AssertExtensions
import java.nio.file.Paths

class CramInterpreterSuite extends munit.FunSuite with AssertExtensions:
    test("Single line"):
        val interpreter = createCramInterpreter()
        interpreter.eval("  $ echo a") `is equal to` Seq("  a")

    test("Multiple lines with state"):
        val interpreter = createCramInterpreter()
        interpreter.eval("  $ MSG=hello") `is equal to` Seq.empty
        interpreter.eval("  $ echo $MSG") `is equal to` Seq("  hello")

    test("Ignore lines not starting with console command prefix"):
        val interpreter = createCramInterpreter()
        interpreter.eval("Just a comment") `is equal to` Seq.empty
        interpreter.eval("  Not starting with $") `is equal to` Seq.empty

    test("Output error code when command fails"):
        val interpreter = createCramInterpreter()
        val Seq(_, errorLine) = interpreter.eval("  $ cat not_existing.txt")
        errorLine `is equal to` "  [1]"

    private def createCramInterpreter() =
        val bashPath =
            if System.getProperty("os.name").toLowerCase().contains("windows")
            then "C:\\Windows\\System32\\bash.exe"
            else "/bin/bash"
        CramInterpreter.Factory.BASH(Paths.get(bashPath)).create()

end CramInterpreterSuite
