package nyub.interpreter

import scala.util.matching.Regex

class ScriptDecorator(
    private val interpreterFactory: InterpreterFactory,
    private val decorationPrefix: String,
    private val config: ScriptDecorator.Config = Config.DEFAULT
):
    def decorate(script: Iterable[String]): Iterable[String] =
        val interpreter = interpreterFactory.create()
        script
            .filterNot(config.erase)
            .flatMap: l =>
                val interpretation = interpreter.eval(l)
                if interpretation.size == 1 && config.inline(interpretation(0))
                then Seq(l + " " + decorationPrefix + interpretation(0))
                else Seq(l) ++ interpretation.map(decorationPrefix + _)

object ScriptDecorator:
    trait Config:
        def inline(outputLine: String): Boolean
        def erase(inputLine: String): Boolean

    object Config:
        val DEFAULT: OverridableDefaults =
            OverridableDefaults(_ => false, _ => false)

        class OverridableDefaults private[Config] (
            private val inlining: String => Boolean,
            private val erasing: String => Boolean
        ) extends Config:
            override def inline(outputLine: String): Boolean = inlining(
              outputLine
            )

            override def erase(inputLine: String): Boolean = erasing(inputLine)

            def withInlining(i: String => Boolean) =
                OverridableDefaults(i, erasing)

            def inlineShorterThan(n: Int) = withInlining: s =>
                inlining(s) || s.length() < n

            def withErasing(e: String => Boolean) =
                OverridableDefaults(inlining, e)

            def eraseStartingWith(prefix: String) = withErasing: s =>
                erasing(s) || s.startsWith(prefix)

            def eraseMatching(regex: Regex): OverridableDefaults = withErasing:
                s => erasing(s) || regex.matches(s)

            def eraseMatching(
                predicate: String => Boolean
            ): OverridableDefaults = withErasing: s =>
                erasing(s) || predicate(s)
