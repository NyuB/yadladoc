package nyub.filesystem

import java.nio.file.Path
import java.nio.file.Files

extension (p: Path)
    def /(other: Path) = p.resolve(other)
    def /(other: String) = p.resolve(other)
    def lines: Iterable[String] =
        val all = Files.readString(p).splitWithDelimiters("(\r\n)|(\n)", 0)
        DelimiterIterator(all).to(List)

private class DelimiterIterator(private val delimited: Array[String])
    extends Iterator[String]:
    private var i = 0
    private var addOneEmptyLine = delimited.length % 2 == 0
    override def hasNext: Boolean = addOneEmptyLine || i < delimited.length
    override def next(): String =
        if i >= delimited.length && addOneEmptyLine then
            addOneEmptyLine = false
            ""
        else
            val res = delimited(i)
            i += 2
            res
