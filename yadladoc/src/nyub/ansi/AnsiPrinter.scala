package nyub.ansi

final class AnsiPrinter(private val printColors: Boolean):
    def print(content: String): String =
        if printColors then content else noColor(content)

    def apply(content: String): String = print(content)
    private def noColor(content: String): String =
        AnsiColors.COLOR_CONTROL_SEQUENCES.foldLeft(content): (acc, color) =>
            acc.replace(color, "")

object AnsiPrinter:
    val NO_COLOR = AnsiPrinter(false)
    val WITH_COLOR = AnsiPrinter(true)
