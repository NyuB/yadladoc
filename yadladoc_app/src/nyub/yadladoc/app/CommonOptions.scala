package nyub.yadladoc.app

import nyub.ansi.AnsiPrinter

case class CommonOptions(val printer: AnsiPrinter)
object CommonOptions:
    val DEFAULTS = CommonOptions(AnsiPrinter.NO_COLOR)
