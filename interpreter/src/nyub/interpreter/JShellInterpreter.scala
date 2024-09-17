package nyub.interpreter

import jdk.jshell.Snippet.Status.{REJECTED, VALID}
import jdk.jshell.{EvalException, JShell, Snippet, SnippetEvent}

import java.lang.ref.Cleaner
import java.util.Locale

/** Java interpreter backed by a [[jdk.jshell.JShell]] instance
  */
class JShellInterpreter extends Interpreter with AutoCloseable:
    private val shell = JShell.builder().executionEngine("local").build()
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
            .toSeq
            .flatMap(snippetEventRepresentation)

    private def snippetEventRepresentation(e: SnippetEvent): Seq[String] =
        e.status() match
            case VALID =>
                val v = e.value()
                if v == null then
                    e.exception() match
                        case null =>
                            Seq.empty // Valid snippet but no output, e.g. for an import or a partial snippet
                        case ex: EvalException =>
                            Seq(ex.getExceptionClassName())
                        case ex => Seq(ex.getMessage())
                else v.split("(\n)|(\r\n)").toIndexedSeq
            case REJECTED =>
                diagnostics(e.snippet())
            case _ => Seq("Unknown error evaluating snippet")

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

    private def diagnostics(snippet: Snippet): Seq[String] =
        shell.diagnostics(snippet).map(_.getMessage(Locale.getDefault())).toSeq

    private val clean: Cleaner.Cleanable =
        JShellInterpreter.cleaner.register(this, () => this.shell.close())

    override def close(): Unit = clean.clean()

    extension [T](jl: java.util.List[T])
        private def toSeq: Seq[T] =
            val l = scala.collection.mutable.ArrayBuffer[T]()
            jl.forEach(l.addOne)
            l.toSeq

    extension [T](jl: java.util.stream.Stream[T])
        private def toSeq: Seq[T] =
            val l = scala.collection.mutable.ArrayBuffer[T]()
            jl.forEach(l.addOne)
            l.toSeq

object JShellInterpreter extends InterpreterFactory:
    def create(): JShellInterpreter = JShellInterpreter()

    private val cleaner = Cleaner.create()
