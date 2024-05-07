package nyub.yadladoc

import scala.annotation.targetName

object Markdown:
    private val MARKDOWN_SNIPPET_PREFIX_CHAR = '`'
    private val MARKDOWN_SNIPPET_PREFIX = "```"

    sealed trait Block

    /** Represents raw lines without additional semantics
      */
    case class Raw(lines: Seq[String]) extends Block
    object Raw:
        @targetName("of")
        def apply(lines: String*) = new Raw(lines.toSeq)

    /** Represents a markdown-formatted code snippet
      *
      * @example
      *   ````markdown
      *   ```scala param1 param2
      *
      *   // some code ...
      *
      *   ```
      *   ````
      * @param header
      *   metadata about the snippet, deduced from the header line
      * @param lines
      *   the actual lines of code in the snippet (not including the \`\`\`
      *   prefixed lines)
      */
    case class Snippet(header: Snippet.Header, lines: Seq[String]) extends Block
    object Snippet:
        @targetName("of")
        def apply(header: Header, lines: String*) =
            new Snippet(header, lines.toSeq)

        case class Header(
            val prefix: String,
            val language: Option[Language],
            val properties: Properties
        )

        private[Markdown] def headerOfLine(line: String): Header =
            val prefix = snippetPrefix(line)
            val language =
                line.substring(prefix.length).takeWhile(c => !" \t".contains(c))
            val properties = line.substring(prefix.length + language.length)
            Header(
              prefix,
              if language.length > 0 then Some(Language.named(language))
              else None,
              Properties.ofLine(properties)
            )

    def parse(input: Iterable[String]): Seq[Block] =
        input
            .foldLeft(BlockParsing.Init.asInstanceOf[BlockParsing]):
                (acc, item) => acc.parse(item)
        match
            case BlockParsing.Init        => Seq.empty
            case r: BlockParsing.RawBlock => r.get()
            case s: BlockParsing.SnippetBlock =>
                throw IllegalStateException("Unclosed code snippet parsing")

    private trait BlockParsing:
        def parse(line: String): BlockParsing

    private object BlockParsing:
        object Init extends BlockParsing:
            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_SNIPPET_PREFIX) then
                    SnippetBlock(
                      List.empty,
                      List.empty,
                      Snippet.headerOfLine(line)
                    )
                else RawBlock(List(line), List.empty)

        class RawBlock(
            private val lines: List[String],
            private val previousBlocks: List[Block]
        ) extends BlockParsing:
            def get(): Seq[Block] =
                withNewRawBlockIFNotEmpty.reverse

            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_SNIPPET_PREFIX) then
                    val raw = Raw(lines.reverse)
                    SnippetBlock(
                      List.empty,
                      withNewRawBlockIFNotEmpty,
                      Snippet.headerOfLine(line)
                    )
                else RawBlock(line :: lines, previousBlocks)

            private def withNewRawBlockIFNotEmpty =
                if lines.isEmpty then previousBlocks
                else
                    val raw = Raw(lines.reverse)
                    raw :: previousBlocks

        class SnippetBlock(
            private val lines: List[String],
            private val previousBlocks: List[Block],
            private val header: Snippet.Header
        ) extends BlockParsing:

            override def parse(line: String): BlockParsing =
                if line.hasSameHeader then
                    val snippet = Snippet(header, lines.reverse)
                    RawBlock(List.empty, snippet :: previousBlocks)
                else SnippetBlock(line :: lines, previousBlocks, header)

            extension (s: String)
                private def hasSameHeader: Boolean =
                    snippetPrefix(s) == header.prefix

    private def snippetPrefix(line: String): String =
        line.takeWhile(_ == MARKDOWN_SNIPPET_PREFIX_CHAR)

end Markdown
