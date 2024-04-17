package nyub.yadladoc

class YadladocSuite extends munit.FunSuite:
    test("Parses empty lines"):
        val input = List.empty[String]
        val parsed = Yadladoc.parse(input)
        parsed isEqualTo List.empty

    test("Only raw markdown"):
        val input =
            l"""
        # Title
        Description
        ## Subtitle
        Text
        """
        Yadladoc.parse(input) isEqualTo List(
          Yadladoc.Raw(Seq("# Title", "Description", "## Subtitle", "Text"))
        )

    extension [T](t: T)
        infix def isEqualTo(other: T): Unit =
            assertEquals(t, other)

    extension (sc: StringContext)
        /**
        * Trim common indent and trailing white spaces
        * Usefull for text block in raw strings
        */
        def l(args: Any*): Iterable[String] =
            var s = sc.s(args*).stripTrailing()
            if s.startsWith("\n") then s = s.substring(1, s.length)
            if s.endsWith("\n") then s = s.substring(0, s.length - 1)
            LineIterable(s)

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
            println(s"Common Indent is [$commonIndent](${commonIndent.length})")
            s.linesIterator.map(l =>
                println(s"Iterating over line '$l'")
                l.substring(commonIndent.length, l.length)
            )
