package nyub.interpreter

import jdk.jshell.{EvalException, JShell, Snippet, SnippetEvent}
import jdk.jshell.Snippet.Status.{OVERWRITTEN, REJECTED, VALID}

class JShellInterpreter extends Interpreter:
    private val shell = jdk.jshell.JShell.create()
    // If not adding the current class path manually, jshell appears to be limited to jdk classes in test suites
    shell.addToClasspath(System.getProperty("java.class.path"))

    override def eval(line: String): Seq[String] =
        shell
            .eval(line)
            .toList
            .map: e =>
                e.status() match
                    case VALID =>
                        val v = e.value()
                        if v == null then
                            val ex = e.exception()
                            if ex == null then None
                            else Some(ex.getCause().toString())
                        else Some(v)
                    case REJECTED => Some("Invalid snippet")
                    case _        => Some("Unknown error")
            .flatMap(_.toList)

    extension [T](jl: java.util.List[T])
        def toList: List[T] =
            val l = scala.collection.mutable.ArrayBuffer[T]()
            jl.forEach(l.addOne)
            l.toList
