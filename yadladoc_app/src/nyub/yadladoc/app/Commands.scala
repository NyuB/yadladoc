package nyub.yadladoc.app

import nyub.ansi.AnsiPrinter
import nyub.yadladoc.{
    ConfigurationConstants,
    ConfigurationFromFile,
    Errors,
    GeneratedFile,
    Results,
    Yadladoc
}

import java.nio.file.{Path, Paths}

type ExitCode = Int
trait Command:
    def parse(args: Seq[String]): Command
    def run(): ExitCode

case class YdocMain(printer: AnsiPrinter = AnsiPrinter.NO_COLOR)
    extends Command:
    override def parse(args: Seq[String]): Command = args match
        case "check" :: rest => Check(Seq.empty, printer = printer).parse(rest)
        case "run" :: rest   => Run(Seq.empty, printer = printer).parse(rest)
        case "help" :: rest  => Help(false).parse(rest)
        case "--color" :: rest =>
            this.copy(printer = AnsiPrinter.WITH_COLOR).parse(rest)
        case Nil => this
        case _   => Help(true)

    override def run(): ExitCode = Help(true).run()

class Help(failureMode: Boolean) extends Command:
    override def parse(args: Seq[String]): Command =
        if args.isEmpty then this else Help(true)

    override def run(): ExitCode =
        println(help)
        if failureMode then INVALID_ARGUMENT_RETURN_CODE else OK_RETURN_CODE

    private val help = """Usage: yadladoc {run|check} [markdown_files, ...]
    run  : generate documentation code from snippets in [markdown_files, ...]
    check: validate that the currrent documentation code is identical to what would be generated by 'run'
Common options:
    --color: use color when printing errors or diffs to the console"""

case class Check(files: Seq[Path], printer: AnsiPrinter = AnsiPrinter.NO_COLOR)
    extends Command:
    override def parse(args: Seq[String]): Command =
        if files.isEmpty && args.isEmpty then Help(true)
        else this.copy(files = files ++ args.map(Paths.get(_)))

    override def run(): ExitCode =
        val outputDir = Paths.get(".")
        withYadladoc: yadladoc =>
            val (checked, errors) =
                applyYadladocOnEachFile(files, yadladoc.check(outputDir, _))
            checked.foreach: g =>
                println(s"Checked ${g.short} generated from ${g.from}")
            printErrorsThenExitCode(errors, printer)

case class Run(files: Seq[Path], printer: AnsiPrinter = AnsiPrinter.NO_COLOR)
    extends Command:
    override def parse(args: Seq[String]): Command =
        if files.isEmpty && args.isEmpty then Help(true)
        else this.copy(files = files ++ args.map(Paths.get(_)))

    override def run(): ExitCode =
        val outputDir = Paths.get(".")
        withYadladoc: yadladoc =>
            val (generated, errors) =
                applyYadladocOnEachFile(files, yadladoc.run(outputDir, _))
            generated.foreach: g =>
                println(s"Generated ${g.short} from ${g.from}")
            printErrorsThenExitCode(errors, printer)

private def withYadladoc(operation: Yadladoc => ExitCode): ExitCode =
    val configDir = Paths.get(".ydoc")
    if !configDir.toFile().isDirectory() then
        println(".ydoc is not a valid directory")
        INVALID_ARGUMENT_RETURN_CODE
    else
        val yadladoc = Yadladoc(
          ConfigurationFromFile(configDir, ConfigurationConstants.DEFAULTS)
        )
        operation(yadladoc)

private def applyYadladocOnEachFile(
    markdownFiles: Seq[Path],
    operation: Path => Results[Seq[GeneratedFile]]
): (Seq[GeneratedFile], Seq[Errors]) =
    markdownFiles.foldLeft(Seq.empty -> Seq.empty): (acc, markdownFile) =>
        val Results(g, e) = operation(markdownFile)
        (acc._1 ++ g) -> (acc._2 ++ e)

private def printErrorsThenExitCode(
    documentationErrors: Iterable[Errors],
    printer: AnsiPrinter
): ExitCode =
    documentationErrors.foreach: e =>
        println(
          s"Error [${e.getClass().getSimpleName()}]: ${printer.print(e.prettyPrintedMessage)}"
        )
    if !documentationErrors.isEmpty then DOCUMENTATION_PROBLEM_RETURN_CODE
    else OK_RETURN_CODE
