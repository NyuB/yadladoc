package nyub.interpreter

import nyub.assert.AssertExtensions

class JShellInterpreterSuite extends munit.FunSuite with AssertExtensions:
    test("Variable declaration and reference"):
        val shell = jshell()
        shell.eval("var one = 1;") `is equal to` List("1")
        shell.eval("one") `is equal to` List("1")

    test("Access to classpath"):
        jshell().eval("new nyub.interpreter.Exposed();") `is equal to` List(
          "nyub.interpreter.Exposed"
        )

    test("Multiple instruction on one line"):
        jshell().eval("var a = 1; var b = 2;") `is equal to` List("1", "2")

    test("One instruction on multiple lines"):
        val shell = jshell()
        shell.eval("var a = ") `is equal to` Seq.empty
        shell.eval("3") `is equal to` Seq("3")

    test("Multi line object representation"):
        jshell().eval("new nyub.interpreter.Multiline();") `is equal to` List(
          "Multi",
          "Line",
          "Representation"
        )

    test("Exception representation"):
        val shell = jshell();
        shell.eval("Object npe = null") `is equal to` Seq("null")
        shell.eval("npe.toString()") `is equal to` Seq(
          "java.lang.NullPointerException"
        )

    test("Invalid snippet representation"):
        jshell().eval("int a = false") `is equal to` Seq(
          "incompatible types: boolean cannot be converted to int"
        )

    test("Array representation"):
        jshell().eval("\"ABC\".split(\"B\")") `is equal to` List(
          "String[2] { \"A\", \"C\" }"
        )

    private def jshell() = JShellInterpreter()

class Exposed:
    override def toString(): String = getClass().getCanonicalName()

class Multiline:
    override def toString(): String = "Multi\nLine\nRepresentation"
