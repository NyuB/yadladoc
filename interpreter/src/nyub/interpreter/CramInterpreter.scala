package nyub.interpreter

import java.nio.file.Path

class CramInterpreter(private val shellInterpreter: Interpreter)
    extends Interpreter:
    override def eval(line: String): Seq[String] =
        if line.startsWith("  $ ") then
            shellInterpreter.eval(line.replace("  $ ", "")).map(s => s"  $s")
        else Seq.empty

object CramInterpreter:
    class Factory(private val shellFactory: InterpreterFactory)
        extends InterpreterFactory:
        override def create(): Interpreter =
            CramInterpreter(shellFactory.create())

    object Factory:
        def BASH(bashPath: Path): Factory = Factory(
          BashInterpreter.Factory(bashPath)
        )
