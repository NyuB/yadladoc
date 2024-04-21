package nyub.yadladoc

import nyub.yadladoc.Markdown.Snippet

class MarkdownSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:
    test("Parses empty lines"):
        val input = List.empty[String]
        val parsed = Markdown.parse(input)
        parsed isEqualTo List.empty

    test("Only raw markdown"):
        val input = l"""
        # Title
        Description
        ## Subtitle
        Text
        """
        Markdown.parse(input) isEqualTo Seq(
          Markdown.Raw(Seq("# Title", "Description", "## Subtitle", "Text"))
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
        Markdown.parse(input) isEqualTo Seq(
          Markdown.Raw("# Title", "This is a code snippet"),
          Markdown.Snippet(
            Markdown.Snippet.Header("```", Some("scala"), Properties.empty),
            "val i: Int = 0"
          ),
          Markdown.Raw("Awesome isn't it ?")
        )

    test("Empty snippet"):
        val input = l"""
        ```
        ```
        """
        Markdown.parse(input) isEqualTo Seq(
          Markdown.Snippet(
            Markdown.Snippet.Header("```", None, Properties.empty)
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
        Markdown.parse(input) isEqualTo Seq(
          Markdown.Snippet(
            Markdown.Snippet.Header("```", Some("markdown"), Properties.empty),
            "You can nest markdown in markdown :O",
            "````java",
            "class Inception {",
            "",
            "}",
            "````"
          )
        )

    test("Header parsing"):
        checkHeaderParsing("```java", Some("java"), Properties.empty)
        checkHeaderParsing(
          "```scala foo=bar baz",
          Some("scala"),
          Properties("foo" -> "bar")
        )
        checkHeaderParsing(
          "``` a=x \tb=y   c=z  ",
          None,
          Properties("a" -> "x", "b" -> "y", "c" -> "z")
        )

    private def checkHeaderParsing(
        headerLine: String,
        expectedLanguage: Option[String],
        expectedProperties: Properties
    ): Unit =
        val input = List(headerLine, "```")
        val parsed = Markdown.parse(input)
        assertEquals(
          parsed.size,
          1,
          s"Expected only one snippet to be parsed but got ${parsed.size}"
        )
        parsed(0) match
            case Snippet(header, _) =>
                header.language isEqualTo expectedLanguage
                header.properties isEqualTo expectedProperties
            case e => fail(s"Expected a snippet header to be parsed but got $e")

end MarkdownSuite
