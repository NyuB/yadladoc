package nyub.interpreter

import jdk.jshell.JShell
import jdk.jshell.Snippet
import jdk.jshell.SnippetEvent
import java.util.Scanner

@main def explore =
    val shell = JShell.create()
    val events = scala.collection.mutable.ArrayBuffer[SnippetEvent]()
    shell.onSnippetEvent(events.addOne)
    var loop = true
    val scanner = Scanner(System.in)
    while loop do
        print("//? ")
        val line = scanner.nextLine()
        if line == null || line == ":exit" || line == ":quit" then loop = false
        else if line.startsWith(":completion ") then
            val subline = line.substring(":completion ".length())
            shell
                .sourceCodeAnalysis()
                .completionSuggestions(
                  subline,
                  subline.length(),
                  Array.ofDim[Int](1)
                )
                .forEach: s =>
                    println(s"\t${s.continuation()}")
        else
            shell.eval(line)
            events.foreach(e => println(e.repr))
            events.clear()

/** To test if application classes are accesible from jshell
  */
class Custom:
    override def toString(): String = "I'm not a built in JDK class"

extension (s: Snippet)
    def repr: String = if s == null then "null"
    else s"kind=${s.kind()}-${s.subKind()} source='${s.source()}' ${s.id()}"

extension (e: SnippetEvent)
    def repr: String =
        if e.exception() != null then e.exception().getMessage()
        else
            s"//\t> value='${e.value()}' ${e.previousStatus()} -> ${e
                    .status()}\n//\t [${e.snippet().repr}]\n//\t [${e.causeSnippet().repr}]"
