package nyub.ansi

import nyub.assert.AssertExtensions

class AnsiPrinterSuite extends munit.FunSuite with AssertExtensions:
    test("Print colorless text as is"):
        AnsiPrinter.WITH_COLOR("Abcdefgh") `is equal to` "Abcdefgh"
        AnsiPrinter.NO_COLOR("Abcdefgh") `is equal to` "Abcdefgh"

    test("Escape colorfull text when printColors = false"):
        val redMinusThenReset = "\u001b[91m-\u001b[0m"
        AnsiPrinter.NO_COLOR(redMinusThenReset) `is equal to` "-"

    test("Print colorfull text as is when printColors = true"):
        val greenPlus = "\u001b[92m+"
        AnsiPrinter.WITH_COLOR(greenPlus) `is equal to` greenPlus

end AnsiPrinterSuite
