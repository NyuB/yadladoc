package nyub.yadladoc

import scala.annotation.targetName

object Markdown:
    private val MARKDOWN_SNIPPET_PREFIX_CHAR = '`'
    private val MARKDOWN_SNIPPET_PREFIX = "```"
    private def snippetPrefix(line: String): String =
        line.takeWhile(_ == MARKDOWN_SNIPPET_PREFIX_CHAR)

    sealed trait Block

    case class Raw(lines: Seq[String]) extends Block
    object Raw:
        @targetName("of")
        def apply(lines: String*) = new Raw(lines.toSeq)

    case class Snippet(header: Snippet.Header, lines: Seq[String]) extends Block
    object Snippet:
        @targetName("of")
        def apply(header: Header, lines: String*) =
            new Snippet(header, lines.toSeq)

        case class Header(
            val prefix: String,
            val language: Option[String],
            val properties: List[String]
        )

        private[Markdown] def headerOfLine(line: String): Header =
            val prefix = snippetPrefix(line)
            val language =
                line.substring(prefix.length).takeWhile(c => !" \t".contains(c))
            val properties = line
                .substring(prefix.length + language.length)
                .split("\\s")
                .filterNot(_.isEmpty)
                .toList
            Header(
              prefix,
              if language.length > 0 then Some(language) else None,
              properties
            )

    def parse(input: Iterable[String]): Seq[Block] =
        input
            .foldLeft(BlockParsing.Init.asInstanceOf[BlockParsing]):
                (acc, item) => acc.parse(item)
            .get()

    private trait BlockParsing:
        def parse(line: String): BlockParsing
        def get(): Seq[Block]

    private object BlockParsing:
        object Init extends BlockParsing:
            override def get(): Seq[Block] = Seq.empty
            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_SNIPPET_PREFIX) then
                    SnippetBLock(
                      List.empty,
                      List.empty,
                      Snippet.headerOfLine(line)
                    )
                else RawBlock(List(line), List.empty)

        class RawBlock(
            private val lines: List[String],
            private val previousBlocks: List[Block]
        ) extends BlockParsing:
            override def get(): Seq[Block] =
                withNewRawBlockIFNotEmpty.reverse

            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_SNIPPET_PREFIX) then
                    val raw = Raw(lines.reverse)
                    SnippetBLock(
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

        class SnippetBLock(
            private val lines: List[String],
            private val previousBlocks: List[Block],
            private val header: Snippet.Header
        ) extends BlockParsing:
            override def get(): Seq[Block] = throw IllegalStateException(
              "Unclosed code snippet parsing"
            )

            override def parse(line: String): BlockParsing =
                if line.hasSameHeader then
                    val snippet = Snippet(header, lines.reverse)
                    RawBlock(List.empty, snippet :: previousBlocks)
                else SnippetBLock(line :: lines, previousBlocks, header)

            extension (s: String)
                private def hasSameHeader: Boolean =
                    snippetPrefix(s) == header.prefix

end Markdown
