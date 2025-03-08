package nyub.yadladoc

import nyub.assert.AssertExtensions
import nyub.markdown.Markdown
import java.nio.file.Paths
import nyub.markdown.Markdown.Snippet.Header
import nyub.markdown.Markdown.Snippet.Prefix

class DocumentationGenerationSuite extends munit.FunSuite with AssertExtensions:
    test("Three example snippets, first two in the same example"):
        val firstSnippet: DocumentationKind.ExampleSnippet =
            DocumentationKind.ExampleSnippet(
              "A",
              Snippet(someLanguage, Seq("a = 1"), Properties.empty)
            )
        val secondSnippet: DocumentationKind.ExampleSnippet =
            DocumentationKind.ExampleSnippet(
              "A",
              Snippet(someLanguage, Seq("b = 1"), Properties.empty)
            )
        val thirdSnippet: DocumentationKind.ExampleSnippet =
            DocumentationKind.ExampleSnippet(
              "B",
              Snippet(someLanguage, Seq("c = 1"), Properties.empty)
            )

        val docGen = DocumentationGeneration
            .init(testConfig)
            .addExampleSnippet(ignoredMdSnippet, firstSnippet)
            .addExampleSnippet(ignoredMdSnippet, secondSnippet)
            .addExampleSnippet(ignoredMdSnippet, thirdSnippet)

        docGen.exampleSnippets.examples("A") `is equal to` Example(
          "A",
          TemplateId(someLanguage.get.name),
          someLanguage,
          Seq(
            bodyOnlyExampleContent(firstSnippet.snippet.lines),
            bodyOnlyExampleContent(secondSnippet.snippet.lines)
          )
        )

    test("Include prefix when prefix property is present"):
        val snippet: DocumentationKind.ExampleSnippet =
            DocumentationKind.ExampleSnippet(
              "A",
              Snippet(
                someLanguage,
                Seq("a = 1"),
                Properties("ydoc.prefix" -> "prefixId")
              )
            )
        val docGen = DocumentationGeneration
            .init(testConfig)
            .addExampleSnippet(ignoredMdSnippet, snippet)
        docGen.exampleSnippets
            .examples("A")
            .content
            .singleElement
            .prefixTemplateIds `is equal to` Seq(TemplateId("prefixId"))

    test("Include suffix when suffix property is present"):
        val snippet: DocumentationKind.ExampleSnippet =
            DocumentationKind.ExampleSnippet(
              "A",
              Snippet(
                someLanguage,
                Seq("a = 1"),
                Properties("ydoc.suffix" -> "suffixId")
              )
            )
        val docGen = DocumentationGeneration
            .init(testConfig)
            .addExampleSnippet(ignoredMdSnippet, snippet)
        docGen.exampleSnippets
            .examples("A")
            .content
            .singleElement
            .suffixTemplateIds `is equal to` Seq(TemplateId("suffixId"))

    test("Markdown blocks are added as is"):
        val docGen = DocumentationGeneration
            .init(testConfig)
            .addRawBlock(Markdown.Raw("# One line"))
        docGen.markdownDecoration.decoratedLines `is equal to` Seq("# One line")

    test("Markdown snippets are added along examples"):
        val docGen = DocumentationGeneration
            .init(testConfig)
            .addExampleSnippet(
              Markdown.Snippet(
                Header(Prefix(3), someLanguage, " after language"),
                Seq("a = 1")
              ),
              ignoredExampleSnippet
            )
        docGen.markdownDecoration.decoratedLines `is equal to` Seq(
          s"```${someLanguage.get.name} after language",
          "a = 1",
          "```"
        )

    test("Value of ydoc.indent is not applied to the generated lines"):
        val docGen = DocumentationGeneration
            .init(testConfig)
            .addExampleSnippet(
              Markdown.Snippet(
                Header(Prefix(3), someLanguage, " ydoc.indent=4"),
                Seq("Content")
              ),
              DocumentationKind.ExampleSnippet(
                "test",
                Snippet(
                  someLanguage,
                  Seq("Content"),
                  Properties.ofLine("ydoc.indent=4")
                )
              )
            )
        docGen.markdownDecoration.decoratedLines `is equal to` Seq(
          s"```${someLanguage.get.name} ydoc.indent=4",
          "Content",
          "```"
        )
        docGen.exampleSnippets
            .examples("test")
            .content
            .iterator
            .next()
            .body `is equal to` Seq("    Content")

    private def bodyOnlyExampleContent(
        lines: Iterable[String]
    ): ExampleContent = ExampleContent(Seq.empty, lines, Seq.empty)

    private def ignoredMdSnippet: Markdown.Snippet = Markdown.Snippet(
      Markdown.Snippet.Header(Markdown.Snippet.Prefix(3), None, ""),
      Seq.empty
    )

    private def ignoredExampleSnippet: DocumentationKind.ExampleSnippet =
        DocumentationKind.ExampleSnippet(
          "A",
          Snippet(someLanguage, Seq("a = 1"), Properties.empty)
        )

    private def testConfig: Configuration =
        ConfigurationFromFile(Paths.get("."), ConfigurationConstants.DEFAULTS)

    private val someLanguage = Some(Language.PYTHON)

    extension [T](it: Iterable[T])
        def singleElement: T =
            val iterator = it.iterator
            iterator.hasNext `is equal to` true
            val res = iterator.next()
            iterator.hasNext `is equal to` false
            res

end DocumentationGenerationSuite
