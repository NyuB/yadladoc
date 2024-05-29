package nyub.yadladoc

import nyub.markdown.Markdown

private class DocumentationGeneration(
    val markdownDecoration: MarkdownDecoration,
    val exampleSnippets: ExampleSnippetMerger
):
    def accumulate(
        markdownSnippet: Markdown.Snippet,
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): DocumentationGeneration =
        DocumentationGeneration(
          markdownDecoration.accumulate(markdownSnippet),
          exampleSnippets.accumulate(exampleSnippet)
        )

    def accumulate(markdownSnippet: Markdown.Snippet): DocumentationGeneration =
        DocumentationGeneration(
          markdownDecoration.accumulate(markdownSnippet),
          exampleSnippets
        )

    def accumulate(markdownRaw: Markdown.Raw): DocumentationGeneration =
        DocumentationGeneration(
          markdownDecoration.accumulate(markdownRaw),
          exampleSnippets
        )

private object DocumentationGeneration:
    def init(config: Configuration): DocumentationGeneration =
        DocumentationGeneration(
          MarkdownDecoration.init,
          ExampleSnippetMerger.init(config)
        )

private class MarkdownDecoration(private val blocks: Seq[Markdown.Block]):
    def accumulate(block: Markdown.Block): MarkdownDecoration =
        MarkdownDecoration(blocks :+ block)

private object MarkdownDecoration:
    def init = MarkdownDecoration(Seq.empty)

private class ExampleSnippetMerger(
    private val config: Configuration,
    val examples: Map[String, Example]
):
    def accumulate(
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): ExampleSnippetMerger =
        val updatedExamples = examples.updatedWith(exampleSnippet.name):
            case None =>
                Some(
                  makeExample(exampleSnippet)
                )
            case Some(previous) =>
                Some(previous.merge(makeExample(exampleSnippet)))
        ExampleSnippetMerger(config, updatedExamples)

    private def makeExample(
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): Example =
        val snippet = exampleSnippet.snippet
        Example(
          exampleSnippet.name,
          config
              .templateId(snippet.language, snippet.properties),
          snippet.language,
          List(
            ExampleContent(
              config.prefixTemplateIds(snippet),
              snippet.lines,
              config.suffixTemplateIds(snippet)
            )
          )
        )

private object ExampleSnippetMerger:
    def init(config: Configuration) =
        ExampleSnippetMerger(config, Map.empty)
