package nyub.interpreter

import nyub.interpreter.InterpreterScriptDecorator.Config.OverridableDefaults

import java.nio.file.Path

object CramDecorator:
    val DEFAULT_CONFIG: OverridableDefaults =
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
