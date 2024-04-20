package nyub.yadladoc

class TemplateSuite extends munit.FunSuite with AssertExtensions:
    test("Line without marker is returned as is"):
        Template(Map.empty).apply("Hello there ;)") isEqualTo "Hello there ;)"
