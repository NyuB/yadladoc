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

private val OK_RETURN_CODE = 0
private val INVALID_ARGUMENT_RETURN_CODE = 1
private val DOCUMENTATION_PROBLEM_RETURN_CODE = 2

@main def main(args: String*) =
    if args.size == 1 && isHelpOption(args(0)) then
        print(help)
        exitWith(OK_RETURN_CODE)

    if args.size < 2 then
        println(s"Error: wrong number of arguments")
        println(help)
        exitWith(INVALID_ARGUMENT_RETURN_CODE)

    val command = args(0)
    if command != "run" && command != "check" then
        println(s"Error: unknown comand ${command}")
        println(help)
        exitWith(INVALID_ARGUMENT_RETURN_CODE)

    val configDir = Paths.get(".ydoc")
    if !configDir.toFile().isDirectory() then
        println(".ydoc is not a valid directory")
        exitWith(INVALID_ARGUMENT_RETURN_CODE)

    val markdownFiles = ensureArgsAreValidFilePathsOrExit(
      args.slice(1, args.size)
    )

    val outputDir = Paths.get(".")
    val yadladoc = Yadladoc(
      ConfigurationFromFile(configDir, ConfigurationConstants.DEFAULTS)
    )
    val printer = AnsiPrinter.NO_COLOR
    if command == "run" then
        val (generated, errors) =
            applyYadladocOnEachFile(markdownFiles, yadladoc.run(outputDir, _))
        generated.foreach: g =>
            println(s"Generated ${g.short} from ${g.from}")
        printErrorsThenExit(errors, printer)
    else if command == "check" then
        val (generated, errors) =
            applyYadladocOnEachFile(markdownFiles, yadladoc.check(outputDir, _))
        generated.foreach: g =>
            println(s"Checked ${g.short} generated from ${g.from}")
        printErrorsThenExit(errors, printer)

private def applyYadladocOnEachFile(
    markdownFiles: Seq[Path],
    operation: Path => Results[Seq[GeneratedFile]]
): (Seq[GeneratedFile], Seq[Errors]) =
    markdownFiles.foldLeft(Seq.empty -> Seq.empty): (acc, markdownFile) =>
        val Results(g, e) = operation(markdownFile)
        (acc._1 ++ g) -> (acc._2 ++ e)

private def ensureArgsAreValidFilePathsOrExit(args: Seq[String]): Seq[Path] =
    val (valid, invalid) =
        args.map(Paths.get(_)).partition(p => p.toFile().exists())
    if invalid.isEmpty then valid
    else
        invalid.foreach(p => println(s"$p is not a valid file"))
        println(help)
        exitWith(INVALID_ARGUMENT_RETURN_CODE)

private def printErrorsThenExit(
    documentationErrors: Iterable[Errors],
    printer: AnsiPrinter
) =
    documentationErrors.foreach: e =>
        println(
          s"Error [${e.getClass().getSimpleName()}]: ${printer.print(e.prettyPrintedMessage)}"
        )
    if !documentationErrors.isEmpty then
        exitWith(DOCUMENTATION_PROBLEM_RETURN_CODE)
    else exitWith(OK_RETURN_CODE)

private def exitWith(errorCode: Int): Nothing =
    System.exit(errorCode)
    throw IllegalStateException("Unreachable code, should have exited before")

private def isHelpOption(arg: String) =
    arg == "--help" ||
        arg == "-h" ||
        arg == "help"

private def help = """Usage: yadladoc {run|check} [markdown_files, ...]
    run  : generate documentation code from snippets in [markdown_files, ...]
    check: validate that the currrent documentation code is identical to what would be generated by 'run'
"""
