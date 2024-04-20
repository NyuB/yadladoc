package nyub.yadladoc

class TemplateInjectionSuite extends munit.FunSuite with AssertExtensions:
    test("Line without marker is returned as is"):
        TemplateInjection(Map.empty).inject(
          "Hello there ;)"
        ) isEqualTo "Hello there ;)"

    test("inject one property"):
        testInjection.inject(
          "Property value = ${{foo}}"
        ) isEqualTo "Property value = boo"

    test("inject two properties"):
        testInjection.inject(
          "first=${{foo}}, second=${{far.faz}}"
        ) isEqualTo "first=boo, second=bar.baz"

    test("Nested properties not supported"):
        TemplateInjection(Map("ab" -> "c", "d" -> "a"))
            .inject("Nested=${{a${{d}}}}") isEqualTo "Nested=}}"

    private val testInjection = TemplateInjection(
      Map("foo" -> "boo", "far.faz" -> "bar.baz", "x23" -> "Two\nLines")
    )

end TemplateInjectionSuite
