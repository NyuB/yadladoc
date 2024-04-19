package nyub.yadladoc

import scala.annotation.targetName

object Yadladoc:
    private val MARKDOWN_CODE_HEADER = "```"
    private def getMarkdownHeader(line: String): String =
        line.takeWhile(_ == '`')

    sealed trait Block

    case class Raw(lines: Seq[String]) extends Block
    object Raw:
        @targetName("of")
        def apply(lines: String*) = new Raw(lines.toSeq)

    case class Snippet(lines: Seq[String]) extends Block
    object Snippet:
        @targetName("of")
        def apply(lines: String*) = new Snippet(lines.toSeq)

    def parse(input: Iterable[String]): Seq[Block] =
        input
            .foldLeft(BlockParsing.Init.asInstanceOf[BlockParsing]):
                (acc, item) => acc.parse(item)
            .get()

    trait BlockParsing:
        def parse(line: String): BlockParsing
        def get(): Seq[Block]

    object BlockParsing:
        object Init extends BlockParsing:
            override def get(): Seq[Block] = Seq.empty
            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_CODE_HEADER) then
                    SnippetBLock(
                      List.empty,
                      List.empty,
                      getMarkdownHeader(line)
                    )
                else RawBlock(List(line), List.empty)

        class RawBlock(
            private val lines: List[String],
            private val previousBlocks: List[Block]
        ) extends BlockParsing:
            override def get(): Seq[Block] =
                withNewRawBlockIFNotEmpty.reverse

            override def parse(line: String): BlockParsing =
                if line.startsWith(MARKDOWN_CODE_HEADER) then
                    val raw = Raw(lines.reverse)
                    SnippetBLock(
                      List.empty,
                      withNewRawBlockIFNotEmpty,
                      getMarkdownHeader(line)
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
            private val header: String
        ) extends BlockParsing:
            override def get(): Seq[Block] = throw IllegalStateException(
              "Unclosed code snippet parsing"
            )

            override def parse(line: String): BlockParsing =
                if line.hasSameHeader then
                    val snippet = Snippet(lines.reverse)
                    RawBlock(List.empty, snippet :: previousBlocks)
                else SnippetBLock(line :: lines, previousBlocks, header)

            extension (s: String)
                private def hasSameHeader: Boolean =
                    getMarkdownHeader(s) == header

end Yadladoc
