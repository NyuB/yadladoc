package nyub.yadladoc

import java.nio.file.{Files, Path, StandardOpenOption}
import java.nio.file.Paths

trait SuiteExtensions extends munit.FunSuite:
    val withTempDir = FunFixture[Path](
      setup = test => Files.createTempDirectory(test.name),
      teardown = dir => rmrf(dir)
    )

    def makeFile(
        tempDir: Path,
        filename: String,
        content: String
    ): Path =
        val res = tempDir.resolve(filename)
        Files.createDirectories(res.getParent())
        Files.write(res, content.getBytes(), StandardOpenOption.CREATE)
        res

    def makeFile(tempDir: Path, filename: String)(
        content: => Iterable[String]
    ): Path =
        makeFile(tempDir, filename, content.mkString("\n"))

    def rmrf(p: Path): Unit =
        if p.toFile().isFile() then Files.delete(p)
        else if p.toFile().isDirectory() then
            val children = p.toFile().list().map(p.resolve(_))
            children.foreach(rmrf)
            Files.delete(p)

    extension (sc: StringContext)
        /** Trim common indent and trailing white spaces Usefull for text block
          * in raw strings
          */
        def l(args: Any*): Iterable[String] =
            var s = sc.s(args*).stripTrailing()
            if s.startsWith("\n") then s = s.substring(1, s.length)
            if s.endsWith("\n") then s = s.substring(0, s.length - 1)
            LineIterable(s)

        def p(args: Any*): Path = Paths.get(sc.s(args*))

    private class LineIterable(s: String) extends Iterable[String]:
        private def getIndent(str: String): String =
            var index: Int = 0
            while index < str.length && (" \t".contains(str(index))) do
                index += 1
            str.substring(0, index)

        private val commonIndent = s.linesIterator
            .foldLeft(getIndent(s)): (i, l) =>
                if l.startsWith(i) then i else getIndent(l)

        override def iterator: Iterator[String] =
            s.linesIterator.map(l => l.substring(commonIndent.length, l.length))

end SuiteExtensions
