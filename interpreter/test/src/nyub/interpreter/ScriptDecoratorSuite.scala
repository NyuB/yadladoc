package nyub.interpreter

import nyub.assert.AssertExtensions
import ScriptDecorator.Config

class ScriptDecoratorSuite extends munit.FunSuite with AssertExtensions:
    val alwaysInline = Config.DEFAULT.withInlining(_ => true)
    test("Given and empty seq return an empty seq"):
        ScriptDecorator(Echo, "> ").decorate(Seq.empty) isEqualTo Seq.empty

    test(
      "Given an inlining config, when eval is a single line, then inline it after the script line prefixed with the decoration prefix"
    ):
        ScriptDecorator(Echo, "> ", alwaysInline).decorate(
          Seq("Hello")
        ) isEqualTo Seq("Hello > Hello")

    test(
      "Given a no inlining config, then output eval result on a separate line prefixed with the prefix"
    ):
        ScriptDecorator(Echo, "> ").decorate(Seq("Hello")) isEqualTo Seq(
          "Hello",
          "> Hello"
        )

    test("Mix of single line and multi line output"):
        val oneTwoDecorator = ScriptDecorator(OneTwo, "> ", alwaysInline)

        oneTwoDecorator.decorate(Seq("Hello", "World")) isEqualTo Seq(
          "Hello > One",
          "World",
          "> One",
          "> Two"
        )

    test("Given an erasing config, do not include erased lines in output"):
        val eraseComment = Config.DEFAULT.eraseStartingWith("//")
        ScriptDecorator(Echo, "> ", eraseComment).decorate(
          Seq("Hey", "//Ignored")
        ) isEqualTo Seq(
          "Hey",
          "> Hey"
        )

    private object Echo extends InterpreterFactory:
        override def create(): Interpreter = new:
            override def eval(line: String): Seq[String] = Seq(line)

    private object OneTwo extends InterpreterFactory:
        override def create(): Interpreter = new:
            var one = true
            override def eval(line: String): Seq[String] =
                val isOne = one
                one = !one
                if isOne then Seq("One") else Seq("One", "Two")

end ScriptDecoratorSuite
