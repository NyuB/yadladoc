package nyub.interpreter

import java.io.PrintStream

trait Interpreter:
    def eval(line: String): Seq[String]

trait InterpreterFactory:
    def create(): Interpreter
