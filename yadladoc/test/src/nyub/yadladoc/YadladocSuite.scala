package nyub.yadladoc

class YadladocSuite extends munit.FunSuite:
    test("Parses empty lines"):
        val input = List.empty[String]
        val parsed = Yadladoc.parse(input)
        parsed isEqualTo List.empty

    test("Only raw markdown"):
        val input = l"""
        # Title
        Description
        ## Subtitle
        Text
        """
        Yadladoc.parse(input) isEqualTo Seq(
          Yadladoc.Raw(Seq("# Title", "Description", "## Subtitle", "Text"))
        )

    test("One snippet"):
        val input = l"""
        # Title
        This is a code snippet
        ```scala
        val i: Int = 0
        ```
        Awesome isn't it ?
        """
        Yadladoc.parse(input) isEqualTo Seq(
          Yadladoc.Raw("# Title", "This is a code snippet"),
          Yadladoc.Snippet("val i: Int = 0"),
          Yadladoc.Raw("Awesome isn't it ?")
        )

    test("Empty snippet"):
        val input = l"""
        ```scala
        ```
        """
        Yadladoc.parse(input) isEqualTo Seq(
          Yadladoc.Snippet()
        )

    test("Nested snippets are kept in outer snippet"):
        val input = l"""
        ```markdown
        You can nest markdown in markdown :O
        ````java
        class Inception {
        
        }
        ````
        ```
        """
        Yadladoc.parse(input) isEqualTo Seq(
          Yadladoc.Snippet(
            "You can nest markdown in markdown :O",
            "````java",
            "class Inception {",
            "",
            "}",
            "````"
          )
        )

    extension [T](t: T)
        infix def isEqualTo(other: T): Unit =
            assertEquals(t, other)

    extension (sc: StringContext)
        /** Trim common indent and trailing white spaces Usefull for text block
          * in raw strings
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
            s.linesIterator.map(l => l.substring(commonIndent.length, l.length))
