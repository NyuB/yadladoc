package nyub.yadladoc.app

import nyub.ansi.AnsiPrinter
import nyub.yadladoc.Properties

case class CommonOptions(
    val printer: AnsiPrinter,
    val overrideProperties: Properties
):
    def overridingProperty(property: (String, String)): CommonOptions =
        this.copy(overrideProperties =
            overrideProperties.extendedWith(property)
        )

object CommonOptions:
    val DEFAULTS = CommonOptions(AnsiPrinter.NO_COLOR, Properties.empty)
