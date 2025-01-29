package nyub.filesystem

import java.nio.file.Path
import java.nio.file.Files

extension (p: Path)
    def /(other: Path) = p.resolve(other)
    def /(other: String) = p.resolve(other)
    def lines: Iterable[String] =
        val res = scala.collection.mutable.ArrayBuffer[String]()
        Files.lines(p).forEach(res.addOne)
        res
