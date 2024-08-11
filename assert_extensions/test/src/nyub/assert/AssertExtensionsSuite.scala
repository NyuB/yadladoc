package nyub.assert

class AssertExtensionsSuite extends munit.FunSuite with AssertExtensions:
    test("42 is equal to 42"):
        42 `is equal to` 42

    test("Seq(1,2) matches Seq(_, 2)"):
        Seq(1, 2) matches:
            case Seq(_, 2) => ()

    test("Seq(1) does not match Seq(_, _)"):
        val err = intercept[AssertionError]:
            Seq(1) matches:
                case Seq(_, _) => ()
        err.getMessage().contains("case Seq(_, _) =>") `is equal to` true

end AssertExtensionsSuite
