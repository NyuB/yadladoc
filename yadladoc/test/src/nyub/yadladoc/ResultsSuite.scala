package nyub.yadladoc

import nyub.assert.AssertExtensions
import java.nio.file.Paths

class ResultsSuite extends munit.FunSuite with AssertExtensions:
    test(
      "Mismatching content error message display both expected and actual content"
    ):
        val actualContent = "Actual content 123456"
        val expectedContent = "Expected content ABCDEF"
        val error = CheckErrors.MismatchingContent(
          somePath,
          actualContent,
          expectedContent
        )

        error.prettyPrintedMessage `contains substring` actualContent
        error.prettyPrintedMessage `contains substring` expectedContent

    test("Merged results share common errors"):
        val someError = MissingDecoratorError("Missing A")
        val anotherError = MissingDecoratorError("Missing B")
        val yetAnotherError = MissingDecoratorError("Missing C")
        val mergeError = MissingDecoratorError("Missing D")

        val rootResult = Results.success(None).withError(someError)
        val leftResult = rootResult.withError(anotherError)
        val rightResult = rootResult.withError(yetAnotherError)

        val merged = leftResult.merge(rightResult)((_, _) =>
            Results((), Seq(mergeError))
        )

        merged.errors `is equal to` Seq(
          someError,
          anotherError,
          yetAnotherError,
          mergeError
        )

    private val somePath = Paths.get("some")
end ResultsSuite
