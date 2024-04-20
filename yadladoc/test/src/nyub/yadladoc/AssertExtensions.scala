package nyub.yadladoc

trait AssertExtensions extends munit.Assertions:
    extension [T](t: T)
        infix def isEqualTo(other: T): Unit =
            assertEquals(t, other)
