package nyub.assert

class AssertExtensionsSuite extends munit.FunSuite with AssertExtensions:
    test("42 is equal to 42"):
        42 isEqualTo 42

end AssertExtensionsSuite
