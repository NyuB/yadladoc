package nyub.interpreter

import nyub.assert.AssertExtensions
import InterpreterScriptDecorator.Config

class InterpreterScriptDecoratorSuite
    extends munit.FunSuite
    with AssertExtensions:
    val alwaysInline = Config.DEFAULT.withInlining(_ => true)
    test("Given and empty seq return an empty seq"):
        InterpreterScriptDecorator(Echo, "> ").decorate(
          Seq.empty
        ) `is equal to` Seq.empty

    test(
      "Given an inlining config, when eval is a single line, then inline it after the script line prefixed with the decoration prefix"
    ):
        InterpreterScriptDecorator(Echo, "> ", alwaysInline).decorate(
          Seq("Hello")
        ) `is equal to` Seq("Hello > Hello")

    test(
      "Given a no inlining config, then output eval result on a separate line prefixed with the prefix"
    ):
        InterpreterScriptDecorator(Echo, "> ").decorate(
          Seq("Hello")
        ) `is equal to` Seq(
          "Hello",
          "> Hello"
        )

    test("Mix of single line and multi line output"):
        val oneTwoDecorator =
            InterpreterScriptDecorator(OneTwo, "> ", alwaysInline)

        oneTwoDecorator.decorate(Seq("Hello", "World")) `is equal to` Seq(
          "Hello > One",
          "World",
          "> One",
          "> Two"
        )

    test("Given an erasing config, do not include erased lines in output"):
        val eraseComment = Config.DEFAULT.eraseStartingWith("//")
        InterpreterScriptDecorator(Echo, "> ", eraseComment).decorate(
          Seq("Hey", "//Ignored")
        ) `is equal to` Seq(
          "Hey",
          "> Hey"
        )

    test("Given a transforming config, pass transformed lines to interpreter"):
        val removeDollars = Config.DEFAULT.withTransform(_.replace("$", ">"))
        InterpreterScriptDecorator(Echo, "", removeDollars).decorate(
          Seq("$ Hey")
        ) `is equal to` Seq("$ Hey", "> Hey")

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

end InterpreterScriptDecoratorSuite
