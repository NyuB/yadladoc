package nyub.assert

class AssertExtensionsSuite extends munit.FunSuite with AssertExtensions:
    test("is equal to (success case)"):
        42 `is equal to` 42

    test("is equal to (failing case)"):
        intercept[AssertionError]:
            42 `is equal to` 0

    test("is true because (success case)"):
        (40 + 2 == 42) `is true because` "That's math"

    test("is true because (failing case)"):
        val err = intercept[AssertionError]:
            42 == 0 `is true because` "That's math"
        err.getMessage() `contains substring` "Expected '42 == 0' to be true because 'That's math' but was false"

    test("is false because (success case)"):
        ("is" == "false") `is false because` "That's spelling"

    test("is false because (failing case)"):
        val err = intercept[AssertionError]:
            "false" == "false" `is false because` "That's spelling"
        err.getMessage() `contains substring` """Expected '"false" == "false"' to be false because 'That's spelling' but was true"""

    test("matches (success case)"):
        Seq(1, 2) matches:
            case Seq(_, 2) => ()

    test("matches (failing case)"):
        val err = intercept[AssertionError]:
            Seq(1) matches:
                case Seq(_, _) => ()
        err.getMessage()
            .contains(
              "case Seq(_, _) =>"
            ) `is true because` "error message must contains expected case clauses"

end AssertExtensionsSuite
