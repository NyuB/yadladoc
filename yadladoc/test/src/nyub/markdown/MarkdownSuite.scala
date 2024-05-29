package nyub.markdown

import nyub.assert.AssertExtensions
import nyub.yadladoc.{Language, SuiteExtensions}
import nyub.markdown.Markdown.Snippet

class MarkdownSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:
    test("Parses empty lines"):
        val input = List.empty[String]
        val parsed = nyub.markdown.Markdown.parse(input)
        parsed isEqualTo List.empty

    test("Only raw markdown"):
        val input = l"""
        # Title
        Description
        ## Subtitle
        Text
        """
        nyub.markdown.Markdown.parse(input) isEqualTo Seq(
          nyub.markdown.Markdown.Raw(
            Seq("# Title", "Description", "## Subtitle", "Text")
          )
        )

    test("One snippet"):
        val input = l"""
        # Title
        This is a code snippet
        ```scala
        val i: Int = 0
        ```
        Awesome isn't it ?
        """
        nyub.markdown.Markdown.parse(input) isEqualTo Seq(
          nyub.markdown.Markdown.Raw("# Title", "This is a code snippet"),
          nyub.markdown.Markdown.Snippet(
            nyub.markdown.Markdown.Snippet
                .Header(
                  prefixOfLength3,
                  Some(Language.SCALA),
                  ""
                ),
            "val i: Int = 0"
          ),
          nyub.markdown.Markdown.Raw("Awesome isn't it ?")
        )

    test("Empty snippet"):
        val input = l"""
        ```
        ```
        """
        nyub.markdown.Markdown.parse(input) isEqualTo Seq(
          nyub.markdown.Markdown.Snippet(
            nyub.markdown.Markdown.Snippet.Header(prefixOfLength3, None, "")
          )
        )

    test("Nested snippets are kept in outer snippet"):
        val input = l"""
        ```markdown
        You can nest markdown in markdown :O
        ````java
        class Inception {
        
        }
        ````
        ```
        """
        nyub.markdown.Markdown.parse(input) isEqualTo Seq(
          nyub.markdown.Markdown.Snippet(
            nyub.markdown.Markdown.Snippet
                .Header(
                  prefixOfLength3,
                  Some(Language.MARKDOWN),
                  ""
                ),
            "You can nest markdown in markdown :O",
            "````java",
            "class Inception {",
            "",
            "}",
            "````"
          )
        )

    test("Header parsing"):
        checkHeaderParsing("```java", Some(Language.JAVA), "")
        checkHeaderParsing(
          "```scala foo=bar baz",
          Some(Language.SCALA),
          " foo=bar baz"
        )
        checkHeaderParsing(
          "``` a=x \tb=y   c=z  ",
          None,
          " a=x \tb=y   c=z  "
        )

    test("toLines: Snippet without language"):
        val snippet = Snippet(
          Snippet.Header(Snippet.Prefix(4), None, " some words"),
          "One line"
        )
        Markdown.toLines(Seq(snippet)) isEqualTo Seq(
          "```` some words",
          "One line",
          "````"
        )

    test("toLines: Snippet with language"):
        val snippet = Snippet(
          Snippet
              .Header(Snippet.Prefix(3), Some(Language.PYTHON), " some words"),
          "print('OK')"
        )
        Markdown.toLines(Seq(snippet)) isEqualTo Seq(
          "```python some words",
          "print('OK')",
          "```"
        )

    private val prefixOfLength3 = nyub.markdown.Markdown.Snippet.Prefix(3)

    private def checkHeaderParsing(
        headerLine: String,
        expectedLanguage: Option[Language],
        expectedAfterLanguage: String
    ): Unit =
        val input = List(headerLine, "```")
        val parsed = nyub.markdown.Markdown.parse(input)
        assertEquals(
          parsed.size,
          1,
          s"Expected only one snippet to be parsed but got ${parsed.size}"
        )
        parsed(0) match
            case nyub.markdown.Markdown.Snippet(header, _) =>
                header.language isEqualTo expectedLanguage
                header.afterLanguage isEqualTo expectedAfterLanguage
            case e => fail(s"Expected a snippet header to be parsed but got $e")

end MarkdownSuite
