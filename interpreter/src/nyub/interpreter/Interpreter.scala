package nyub.interpreter

import java.io.PrintStream

/** Statefull 'REPL like' interpreter
  * @see
  *   InterpreterFactory
  */
trait Interpreter:
    /** Evaluate the line, possibly modifying this [[Interpreter]] internal
      * state and return revaluation result representations as lines
      *
      * @param line
      * @return
      */
    def eval(line: String): Seq[String]

trait InterpreterFactory:
    /** Initialize a new [[Interpreter]] Even if the interpreter itself can be
      * statefull, [[InterpreterFactory]]s should be stateless and [[create]]d
      * instances mutually independant
      * @return
      *   an [[Interpreter]] in initial state
      */
    def create(): Interpreter
