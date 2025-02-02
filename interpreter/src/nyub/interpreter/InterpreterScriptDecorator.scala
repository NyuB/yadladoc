package nyub.interpreter

import scala.util.matching.Regex

/** Decorate scripts with the results of their evaluation by an [[Interpreter]]
  * An intended usage is to add objects string representation as comments after
  * each script expression, as the example below with the [[JShellInterpreter]]:
  *
  * {{{
  * "ABC".split("B")
  * // String[2] { "A", "C" }
  * }}}
  *
  * @param interpreterFactory
  *   the interpreter to use when evaluating lines. A new interpreter will be
  *   initialized by a call to [[InterpreterFactory#create]] for each call to
  *   [[decorate]]
  * @param decorationPrefix
  *   the characters to insert before a decoration line, or between a script
  *   line and an inlined decoration
  * @param config
  *   which and how lines and decorations should be arranged. See
  *   [[ScriptDecorator.Config]] for details
  */
class InterpreterScriptDecorator(
    private val interpreterFactory: InterpreterFactory,
    private val decorationPrefix: String,
    private val config: InterpreterScriptDecorator.Config = Config.DEFAULT
) extends ScriptDecorator:
    override def decorate(script: Iterable[String]): Iterable[String] =
        val interpreter = interpreterFactory.create()
        script
            .filterNot(config.erase)
            .flatMap: l =>
                val interpretation = interpreter.eval(config.transform(l))
                if interpretation.size == 1 && config.inline(interpretation(0))
                then Seq(l + " " + decorationPrefix + interpretation(0))
                else Seq(l) ++ interpretation.map(decorationPrefix + _)

object InterpreterScriptDecorator:
    /** Configure how lines and decorations should be arranged.
      */
    trait Config:
        /** Decide wether a single line output should be inlined (put next to
          * the script line that produced it) or put on a new line in the
          * decorated output
          *
          * `inline("abc") == true`
          * ```
          * "abc".toUpperCase() // "ABC"
          * ```
          *
          * VS
          *
          * `inline("abc") == false`
          * ```
          * "abc".toUpperCase()
          * // "ABC"
          * ```
          * Note that multi-line output will always be put on new lines
          * @param outputLine
          *   the single output line produced by a script's line evaluation
          * @return
          *   `true` if `outputLine` should be put on the same line that the
          *   evaluated line, `false` if it should be put on a new line
          */
        def inline(outputLine: String): Boolean

        /** Decide wether or not to consider a script line for evaluation. When
          * `erase(line) == true`, the line will not be evaluated nor included
          * in the decorated output
          *
          * @param inputLine
          *   a script line
          * @return
          *   `false` if `inputLine` should be evaluated and kept in the
          *   decorated output, `true` if it should be discarded
          */
        def erase(inputLine: String): Boolean

        def transform(inputLine: String): String

    object Config:
        val DEFAULT: OverridableDefaults =
            OverridableDefaults(_ => false, _ => false, identity)

        class OverridableDefaults private[Config] (
            private val inlining: String => Boolean,
            private val erasing: String => Boolean,
            private val transforming: String => String
        ) extends Config:
            override def inline(outputLine: String): Boolean = inlining(
              outputLine
            )

            override def erase(inputLine: String): Boolean = erasing(inputLine)

            override def transform(inputLine: String): String = transforming(
              inputLine
            )

            /** Set a new inlining policy
              * @param newInlining
              *   an inlining policy overriding this [[inline]] method
              * @return
              *   a copy of this configuration with the given `newInlining`
              *   policy
              */
            def withInlining(
                newInlining: String => Boolean
            ): OverridableDefaults =
                OverridableDefaults(newInlining, erasing, transforming)

            /** Add an inlining criteria to this configuration.
              *
              * Inline output lines with less than `n` characters. Noe that the
              * resulting inlined line could still be over `n` characters
              *
              * @param n
              *   line length threshold for inlining
              * @return
              *   a copy of this configuration with an **additional** inlining
              *   criteria
              */
            def inlineShorterThan(n: Int): OverridableDefaults = withInlining:
                s => inlining(s) || s.length() < n

            /** Set a new erasing policy
              * @param newErasing
              *   an erasing policy overriding this [[erase]] method
              * @return
              *   a copy of this configuration with the given `newErasing`
              *   policy
              */
            def withErasing(
                newErasing: String => Boolean
            ): OverridableDefaults =
                OverridableDefaults(inlining, newErasing, transforming)

            /** Add an erasing criteria to this configuration.
              *
              * @param prefix
              *   erase output lines starting with this prefix
              * @return
              *   a copy of this configuration with an **additional** erasing
              *   criteria based on the line prefix
              */
            def eraseStartingWith(prefix: String): OverridableDefaults =
                withErasing: s =>
                    erasing(s) || s.startsWith(prefix)

            /** Add an erasing criteria to this configuration.
              *
              * @param regex
              *   erase output lines matching this regular expression
              * @return
              *   a copy of this configuration with an **additional** erasing
              *   criteria based on a regular expression
              */
            def eraseMatching(regex: Regex): OverridableDefaults = withErasing:
                s => erasing(s) || regex.matches(s)

            /** Add an erasing criteria to this configuration.
              *
              * @param predicate
              *   erase output lines verifying this predicate
              * @return
              *   a copy of this configuration with an **additional** erasing
              *   criteria
              */
            def eraseMatching(
                predicate: String => Boolean
            ): OverridableDefaults = withErasing: s =>
                erasing(s) || predicate(s)

            /** Set a new transform operation to apply to the input lines
              *
              * @param newTransform
              *   transform to apply to each input line before interpreting it
              * @return
              *   a copy of this configuration with a new transform operation
              */
            def withTransform(
                newTransform: String => String
            ): OverridableDefaults =
                OverridableDefaults(inlining, erasing, newTransform)
