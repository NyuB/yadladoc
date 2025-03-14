package nyub.markdown

import nyub.yadladoc.Language

import scala.annotation.targetName

object Markdown:
    /** @param blocks
      *   markdown elements' representation
      * @return
      *   the `blocks` as raw lines, one string per line
      */
    def toLines(blocks: Seq[Block]): Iterable[String] =
        blocks.flatMap(_.toLines)

    /** @param input
      *   markdown lines
      * @return
      *   `input` parsed as markdown elements
      */
    def parse(input: Iterable[String]): Seq[Block] =
        input
            .foldLeft(BlockParsing.Init.asInstanceOf[BlockParsing]):
                (acc, item) => acc.parse(item)
        match
            case BlockParsing.Init        => Seq.empty
            case r: BlockParsing.RawBlock => r.get()
            case s: BlockParsing.SnippetBlock =>
                throw IllegalStateException("Unclosed code snippet parsing")

    sealed trait Block:
        private[Markdown] def toLines: Iterable[String]

    /** Represents raw lines without additional semantics
      */
    case class Raw(lines: Iterable[String]) extends Block:
        override def toLines: Iterable[String] = lines

    object Raw:
        @targetName("of")
        def apply(lines: String*): Raw = new Raw(lines.toSeq)

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
    case class Snippet(header: Snippet.Header, lines: Iterable[String])
        extends Block:
        override def toLines: Iterable[String] =
            Seq(header.toLine) ++ lines :+ header.prefix.prefixString

    object Snippet:
        val PREFIX_CHAR: Char = '`'
        val PREFIX_MIN: String = "```"
        case class Header(
            val prefix: Prefix,
            val language: Option[Language],
            val afterLanguage: String
        ):
            def toLine: String =
                val afterPrefix = language
                    .map(l => s"${l.name}${afterLanguage}")
                    .getOrElse(afterLanguage)
                s"${prefix.prefixString}${afterPrefix}"

        case class Prefix(val length: Int):
            def prefixString: String = String(Array.fill(length)(PREFIX_CHAR))

        @targetName("of")
        def apply(header: Header, lines: String*): Snippet =
            new Snippet(header, lines.toSeq)

        private[Markdown] def headerOfLine(line: String): Header =
            val prefix = Snippet.prefix(line)
            val language =
                line.substring(prefix.length).takeWhile(c => !" \t".contains(c))
            val afterLanguage = line.substring(prefix.length + language.length)
            Header(
              prefix,
              if language.length > 0 then Some(Language.named(language))
              else None,
              afterLanguage
            )

        private[Markdown] def isSnippetHeaderLine(line: String): Boolean =
            line.startsWith(PREFIX_MIN)

        def prefix(line: String): Prefix =
            val length = line.takeWhile(_ == PREFIX_CHAR).length()
            Prefix(length)

    private trait BlockParsing:
        def parse(line: String): BlockParsing

    private object BlockParsing:
        object Init extends BlockParsing:
            override def parse(line: String): BlockParsing =
                if Snippet.isSnippetHeaderLine(line) then
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
                if Snippet.isSnippetHeaderLine(line) then
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
                    Snippet.prefix(s) == header.prefix

end Markdown
