package nyub.yadladoc.app

import nyub.assert.AssertExtensions
import java.nio.file.Paths
import nyub.ansi.AnsiPrinter
import nyub.yadladoc.Properties

class CommandsSuite extends munit.FunSuite with AssertExtensions:
    test("help"):
        YdocMain().parse(Seq("help")) `is equal to` Help(false)

    test("help does not expect any more argument"):
        YdocMain().parse(Seq("help", "README.md")) `is equal to` Help(true)

    test("run <file>"):
        YdocMain().parse(Seq("run", "README.md")) `is equal to` Run(
          Seq(Paths.get("README.md"))
        )

    test("run expects at least one path argument"):
        YdocMain().parse(Seq("run")) `is equal to` Help(true)

    test("check <file>"):
        YdocMain().parse(Seq("check", "README.md")) `is equal to` Check(
          Seq(Paths.get("README.md"))
        )

    test("check expects at least one path argument"):
        YdocMain().parse(Seq("check")) `is equal to` Help(true)

    test("--color option activate ansi color codes"):
        val defaultOptionsWithColor =
            CommonOptions.DEFAULTS.copy(printer = AnsiPrinter.WITH_COLOR)
        YdocMain().parse(
          Seq("--color", "check", "README.md")
        ) `is equal to` Check(
          Seq(Paths.get("README.md")),
          defaultOptionsWithColor
        )
        YdocMain().parse(Seq("--color", "run", "README.md")) `is equal to` Run(
          Seq(Paths.get("README.md")),
          defaultOptionsWithColor
        )
        YdocMain().parse(Seq("--color", "help")) `is equal to` Help(
          false,
          defaultOptionsWithColor
        )

    test("-Dkey=value overrides default properties"):
        val check = YdocMain().parse(Seq("-Dkey=value", "check", "README.md"))
        val run = YdocMain().parse(Seq("-Dkey=value", "run", "README.md"))
        val help = YdocMain().parse(Seq("-Dkey=value", "help"))
        extension (props: Properties)
            def `were overriden` = props.get("key") `is equal to some` "value"

        check matches:
            case Check(_, CommonOptions(_, props)) =>
                props.`were overriden`
        run matches:
            case Run(_, CommonOptions(_, props)) =>
                props.`were overriden`
        help matches:
            case Help(_, CommonOptions(_, props)) =>
                props.`were overriden`

end CommandsSuite
