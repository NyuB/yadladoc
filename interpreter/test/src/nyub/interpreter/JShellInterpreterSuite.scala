package nyub.interpreter

import nyub.assert.AssertExtensions

class JShellInterpreterSuite extends munit.FunSuite with AssertExtensions:
    private val decorator = ScriptDecorator(TestInterpreterFactory, "//> ")

    test("Single integer"):
        val script = List(
          "var one = 1;",
          "one"
        )

        decorator.decorate(script) isEqualTo List(
          "var one = 1; //> 1",
          "one //> 1"
        )

    test("JShell has access to classpath"):
        val script = List(
          "import nyub.interpreter.Exposed;",
          "new Exposed(42L);"
        )

        decorator.decorate(script) isEqualTo List(
          "import nyub.interpreter.Exposed;",
          "new Exposed(42L); //> !! Custom Object [42]"
        )

    test("Multiple instruction on one line"):
        val script = List(
          "var a = 1; var b = 2;"
        )

        decorator.decorate(script) isEqualTo List(
          "var a = 1; var b = 2;",
          "//> 1",
          "//> 2"
        )

    test("One instruction on multiple lines"):
        val script = List(
          "var a =",
          "2",
          "a"
        )

        decorator.decorate(script) isEqualTo List(
          "var a =",
          "2 //> 2",
          "a //> 2"
        )

private object TestInterpreterFactory extends InterpreterFactory:
    override def create(): Interpreter = JShellInterpreter()

class Exposed(val id: Long):
    override def toString(): String = s"!! Custom Object [$id]"
