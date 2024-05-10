package nyub.interpreter

class ScriptDecorator(
    private val interpreterFactory: InterpreterFactory,
    private val decorationPrefix: String
):
    def decorate(script: Iterable[String]): Iterable[String] =
        val interpreter = interpreterFactory.create()
        script.flatMap: l =>
            val interpretation = interpreter.eval(l)
            if interpretation.size == 1 then
                Seq(l + " " + decorationPrefix + interpretation(0))
            else Seq(l) ++ interpretation.map(decorationPrefix + _)
