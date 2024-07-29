package nyub.interpreter

import java.nio.file.Path

object CramDecorator:
    val DEFAULT_CONFIG = ScriptDecorator.Config.DEFAULT.eraseMatching(s =>
        s.startsWith("  ") && !s.startsWith("  $ ")
    )

    def apply(
        bashPath: Path,
        config: ScriptDecorator.Config = DEFAULT_CONFIG
    ): ScriptDecorator =
        ScriptDecorator(CramInterpreter.Factory.BASH(bashPath), "", config)
