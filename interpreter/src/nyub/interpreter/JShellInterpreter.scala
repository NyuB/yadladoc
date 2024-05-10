package nyub.interpreter

import jdk.jshell.{EvalException, JShell, Snippet, SnippetEvent}
import jdk.jshell.Snippet.Status.{OVERWRITTEN, REJECTED, VALID}

class JShellInterpreter extends Interpreter:
    private val shell = JShell.create()
    // If not adding the current class path manually, jshell appears to be limited to jdk classes in test suites
    shell.addToClasspath(System.getProperty("java.class.path"))

    /** Part of previous lines that could not be fully evaluated
      */
    private var incomplete = ""

    override def eval(line: String): Seq[String] =
        val all = incomplete + line
        incomplete = ""
        splitSnippets(all).flatMap(evalSingleSnippet)

    private def evalSingleSnippet(line: String): Seq[String] =
        shell
            .eval(line)
            .toList
            .flatMap: e =>
                e.status() match
                    case VALID =>
                        val v = e.value()
                        if v == null then
                            val ex = e.exception()
                            if ex == null then
                                Seq.empty // Valid snippet but no output, e.g. for an import or a partial snippet
                            else Seq(ex.getCause().toString())
                        else v.split("(\n)|(\r\n)")
                    case REJECTED => Seq("Invalid snippet")
                    case _        => Seq("Unknown error")

    private def splitSnippets(line: String): Seq[String] =
        val sourceAnalysis = shell.sourceCodeAnalysis()
        var remaining = line
        val res = scala.collection.mutable.ArrayBuffer[String]()
        while remaining != "" do
            val analysis = sourceAnalysis.analyzeCompletion(remaining)
            if analysis.completeness().isComplete() then
                res.addOne(analysis.source())
                remaining = analysis.remaining()
            else
                incomplete = remaining
                remaining = ""
        res.toList

    extension [T](jl: java.util.List[T])
        private def toList: List[T] =
            val l = scala.collection.mutable.ArrayBuffer[T]()
            jl.forEach(l.addOne)
            l.toList
