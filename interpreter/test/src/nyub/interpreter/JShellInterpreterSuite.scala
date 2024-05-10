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
          "var one = 1;",
          "//> 1",
          "one",
          "//> 1"
        )

private object TestInterpreterFactory extends InterpreterFactory:
    override def create(): Interpreter = JShellInterpreter()
