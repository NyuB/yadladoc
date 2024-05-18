package nyub.interpreter

import nyub.assert.AssertExtensions

class JShellInterpreterSuite extends munit.FunSuite with AssertExtensions:
    test("Variable declaration and reference"):
        val shell = jshell()
        shell.eval("var one = 1;") isEqualTo List("1")
        shell.eval("one") isEqualTo List("1")

    test("Access to classpath"):
        jshell().eval("new nyub.interpreter.Exposed();") isEqualTo List(
          "nyub.interpreter.Exposed"
        )

    test("Multiple instruction on one line"):
        jshell().eval("var a = 1; var b = 2;") isEqualTo List("1", "2")

    test("One instruction on multiple lines"):
        val shell = jshell()
        shell.eval("var a = ") isEqualTo Seq.empty
        shell.eval("3") isEqualTo Seq("3")

    test("Multi line object representation"):
        jshell().eval("new nyub.interpreter.Multiline();") isEqualTo List(
          "Multi",
          "Line",
          "Representation"
        )

    test("Exception representation"):
        val shell = jshell();
        shell.eval("Object npe = null") isEqualTo Seq("null")
        shell.eval("npe.toString()") isEqualTo Seq(
          "java.lang.NullPointerException"
        )

    test("Invalid snippet representation"):
        jshell().eval("int a = false") isEqualTo Seq(
          "incompatible types: boolean cannot be converted to int"
        )

    test("Array representation"):
        jshell().eval("\"ABC\".split(\"B\")") isEqualTo List(
          "String[2] { \"A\", \"C\" }"
        )

    private def jshell() = JShellInterpreter()

class Exposed:
    override def toString(): String = getClass().getCanonicalName()

class Multiline:
    override def toString(): String = "Multi\nLine\nRepresentation"
