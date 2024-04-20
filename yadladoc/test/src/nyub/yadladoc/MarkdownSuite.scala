package nyub.yadladoc

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
            Markdown.Snippet.Header("```", Some("scala")),
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
          Markdown.Snippet(Markdown.Snippet.Header("```", None))
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
            Markdown.Snippet.Header("```", Some("markdown")),
            "You can nest markdown in markdown :O",
            "````java",
            "class Inception {",
            "",
            "}",
            "````"
          )
        )

end MarkdownSuite
