package nyub.yadladoc.templating

import nyub.assert.AssertExtensions

class SurroundingTemplateInjectionSuite
    extends munit.FunSuite
    with AssertExtensions:
    test("Line without marker is returned as is"):
        SurroundingTemplateInjection(prefix = "$", postfix = "$").inject(
          "Hello there ;)",
          Map.empty
        ) isEqualTo "Hello there ;)"

    test("inject one property"):
        testInjection(
          "Property value = {{foo}}"
        ) isEqualTo "Property value = boo"

    test("inject two properties"):
        testInjection(
          "first={{foo}}, second={{far.faz}}"
        ) isEqualTo "first=boo, second=bar.baz"

    test("Nested properties not supported"):
        SurroundingTemplateInjection(prefix = "{{", postfix = "}}")
            .inject(
              "Nested={{a{{d}}}}",
              Map("ab" -> "c", "d" -> "a")
            ) isEqualTo "Nested=}}"

    private def testInjection(line: String) =
        SurroundingTemplateInjection(prefix = "{{", postfix = "}}").inject(
          line,
          Map("foo" -> "boo", "far.faz" -> "bar.baz", "x23" -> "Two\nLines")
        )

end SurroundingTemplateInjectionSuite
