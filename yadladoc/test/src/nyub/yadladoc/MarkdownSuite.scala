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
            Markdown.Snippet.Header("```", Some("scala"), List.empty),
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
          Markdown.Snippet(Markdown.Snippet.Header("```", None, List.empty))
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
            Markdown.Snippet.Header("```", Some("markdown"), List.empty),
            "You can nest markdown in markdown :O",
            "````java",
            "class Inception {",
            "",
            "}",
            "````"
          )
        )

    test("Header parsing"):
        checkHeaderParsing("```java", Some("java"), List.empty)
        checkHeaderParsing(
          "```scala foo bar baz",
          Some("scala"),
          List("foo", "bar", "baz")
        )
        checkHeaderParsing("``` a \tb   c  ", None, List("a", "b", "c"))

    private def checkHeaderParsing(
        headerLine: String,
        expectedLanguage: Option[String],
        expectedProperties: List[String]
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
