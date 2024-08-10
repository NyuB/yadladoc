package nyub.assert

import java.nio.file.{Files, Path}
trait AssertExtensions extends munit.Assertions:
    extension [A](a: A)
        infix def `is equal to`(other: A): Unit =
            assertEquals(a, other)

        infix def matches(pf: PartialFunction[A, Unit]): Unit =
            pf.applyOrElse(
              a,
              _ => fail("$a does not match the expected clauses")
            )

    extension [T](t: Option[T])
        infix def `is equal to some`(other: T): Unit =
            assertEquals(t, Some(other))

    extension [T](l: Iterable[T])
        infix def `contains exactly in any order`(expected: Iterable[T]): Unit =
            assertEquals(
              l.size,
              expected.size,
              s"Expected to contains exactly the elements of ${expected} but sizes do not match (actual=${l.size} expected=${expected.size})"
            )
            expected.foreach(item =>
                assertEquals(
                  l.exists(_ == item),
                  true,
                  s"actual ${l} does not contain expected item ${item} from ${expected}"
                )
            )

    extension (s: String)
        infix def `is equal to lines`(lines: Iterable[String]): Unit =
            assertEquals(s, lines.mkString("\n"))

        infix def `contains substring`(subString: String): Unit =
            assertEquals(
              s.contains(subString),
              true,
              s"Expected ${s} to contain ${subString}"
            )

    extension (p: Path)
        infix def `has content`(content: String): Unit =
            assertEquals(p.toFile().isFile(), true, s"$p is not a file")
            Files.readString(p) `is equal to` content

        infix def `has content`(content: Iterable[String]): Unit =
            p `has content` content.mkString("\n")
