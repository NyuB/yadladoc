package nyub.yadladoc

object Yadladoc:
    sealed trait Block
    case class Raw(lines: Seq[String]) extends Block
    case class Snippet(lines: Seq[String]) extends Block
    case class Directive(lines: Seq[String]) extends Block

    def parse(lines: Iterable[String]): Seq[Block] =
        if lines.isEmpty then Seq.empty else Seq(Raw(lines.toSeq))

end Yadladoc
