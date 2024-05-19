package nyub.assert.examples
import nyub.assert.AssertExtensions
import java.nio.file.Files

class assert_extensions_test_src_nyub_assert_examples_assertions_scala extends munit.FunSuite with AssertExtensions:
    test("assert_extensions_test_src_nyub_assert_examples_assertions_scala_0"):
        42 isEqualTo 42
        assertEquals(42, 42)
    test("assert_extensions_test_src_nyub_assert_examples_assertions_scala_2"):
        val file = Files.createTempDirectory("test").resolve("ok.txt")
        Files.writeString(file, "Line1\nLine2")
        file hasContent "Line1\nLine2" // entire content
        file hasContent List("Line1", "Line2") // line by line
        assertEquals(Files.readString(file), "Line1\nLine2")

end assert_extensions_test_src_nyub_assert_examples_assertions_scala