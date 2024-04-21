package nyub.yadladoc.example

import nyub.yadladoc.AssertExtensions

class GeneratedExample extends munit.FunSuite with AssertExtensions:
    test("Example"):
        42 isEqualTo 42
        assertEquals(42, 42)

end GeneratedExample