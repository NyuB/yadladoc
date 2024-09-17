package nyub.yadladoc

import nyub.markdown.Markdown

private class DocumentationGeneration(
    val markdownDecoration: MarkdownDecoration,
    val exampleSnippets: ExampleSnippetMerger
):
    def addExampleSnippet(
        markdownSnippet: Markdown.Snippet,
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): DocumentationGeneration =
        DocumentationGeneration(
          markdownDecoration.accumulate(markdownSnippet),
          exampleSnippets.accumulate(exampleSnippet)
        )

    def addRawBlock(markdownRaw: Markdown.Block): DocumentationGeneration =
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

    def decoratedLines = Markdown.toLines(blocks)

private object MarkdownDecoration:
    def init = MarkdownDecoration(Seq.empty)

private class ExampleSnippetMerger(
    private val config: Configuration,
    val examples: Map[String, Example]
):
    def accumulate(
        exampleSnippet: DocumentationKind.ExampleSnippet
    ): ExampleSnippetMerger =
        val example = makeExample(exampleSnippet)
        val updatedExamples = examples.updatedWith(exampleSnippet.name):
            case None           => Some(example)
            case Some(previous) => Some(previous.merge(example))
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
