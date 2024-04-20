package nyub.yadladoc

import java.nio.file.{Files, Path}
trait AssertExtensions extends munit.Assertions:
    extension [T](t: T)
        infix def isEqualTo(other: T): Unit =
            assertEquals(t, other)

    extension (s: String)
        infix def isEqualToLines(lines: Iterable[String]): Unit =
            assertEquals(s, lines.mkString("\n"))

    extension (p: Path)
        infix def hasContent(content: String): Unit =
            assertEquals(p.toFile().isFile(), true, s"$p is not a file")
            Files.readString(p) isEqualTo content

        infix def hasContent(content: Iterable[String]): Unit =
            p hasContent content.mkString("\n")
