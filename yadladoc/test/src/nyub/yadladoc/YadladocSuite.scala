package nyub.yadladoc

class YadladocSuite extends munit.FunSuite:
    test("Parses empty lines"):
        val input = List.empty[String]
        val parsed = Yadladoc.parse(input)
        parsed isEqualTo List.empty

    extension [T](t: T)
        def isEqualTo(other: T): Unit =
            assertEquals(t, other)
