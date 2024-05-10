package nyub.interpreter

import nyub.assert.AssertExtensions

class ScriptDecoratorSuite extends munit.FunSuite with AssertExtensions:
    private val echoDecorator = ScriptDecorator(Echo, "> ", Some(" # "))
    test("Given and empty seq return an empty seq"):
        echoDecorator.decorate(Seq.empty) isEqualTo Seq.empty

    test(
      "Given an inline separator, when eval is a single line, then inline it after the script line"
    ):
        echoDecorator.decorate(Seq("Hello")) isEqualTo Seq("Hello # Hello")

    test(
      "Given a prefix and no inline separator, then output eval result on a separate line prefixed with the prefix"
    ):
        val echoDecoratorNoInline = ScriptDecorator(Echo, "> ", None)

        echoDecoratorNoInline.decorate(Seq("Hello")) isEqualTo Seq(
          "Hello",
          "> Hello"
        )

    test("Mix of single line and multi line output"):
        val oneTwoDecorator = ScriptDecorator(OneTwo, "> ", Some(" # "))

        oneTwoDecorator.decorate(Seq("Hello", "World")) isEqualTo Seq(
          "Hello # One",
          "World",
          "> One",
          "> Two"
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
