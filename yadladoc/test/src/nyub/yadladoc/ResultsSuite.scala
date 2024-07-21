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

        error.prettyPrintedMessage.contains(actualContent) `is equal to` true
        error.prettyPrintedMessage.contains(expectedContent) `is equal to` true

    private val somePath = Paths.get("some")
end ResultsSuite
