package nyub.interpreter

import java.nio.file.Path

object CramDecorator:
    val DEFAULT_CONFIG =
        InterpreterScriptDecorator.Config.DEFAULT.eraseMatching(s =>
            s.startsWith("  ") && !s.startsWith("  $ ")
        )

    def apply(
        bashPath: Path,
        config: InterpreterScriptDecorator.Config = DEFAULT_CONFIG
    ): InterpreterScriptDecorator =
        InterpreterScriptDecorator(
          CramInterpreter.Factory.BASH(bashPath),
          "",
          config
        )
