package nyub.filesystem

import nyub.assert.AssertExtensions

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop._

import java.nio.file.{Path, Paths}
import org.scalacheck.Test.Parameters

class FileSystemSuite extends munit.ScalaCheckSuite with AssertExtensions:
    override def scalaCheckInitialSeed =
        "CTH6hXj8ViScMmsO78-k4_RytXHPK_wSJYNH2h4dCpB="

    override protected def scalaCheckTestParameters: Parameters =
        super.scalaCheckTestParameters.withMinSuccessfulTests(15)

    case class Content(s: String)
    case class ContentList(l: List[Content]):
        def apply(i: Int) = l(i)
        def size = l.size

    given Arbitrary[Content] = Arbitrary(
      Gen.choose(0, 10)
          .flatMap(Gen.stringOfN(_, Gen.alphaNumChar).map(Content(_)))
    )

    given Arbitrary[ContentList] = Arbitrary(
      Gen.choose(5, 20)
          .flatMap(
            Gen.listOfN(_, summon[Arbitrary[Content]].arbitrary)
                .map(ContentList(_))
          )
    )

    case class PathList(l: List[Path])
    given Arbitrary[Path] = Arbitrary(
      Gen.choose(1, 10)
          .flatMap(Gen.listOfN(_, Gen.stringOfN(5, Gen.alphaNumChar)))
          .map(l => Paths.get(l.head, l.tail*))
    )

    given Arbitrary[PathList] = Arbitrary(
      Gen.choose(0, 20)
          .flatMap(Gen.listOfN(_, summon[Arbitrary[Path]].arbitrary))
          .map(PathList(_))
    )

    property("""
After arbitrary writes to an actual temp dir mirrored to an in memory file system,
when comparing the in memory file system and the temp dir,
then they are the same"""):
        forAll: (paths: PathList, contents: ContentList) =>
            val os = OsFileSystem()
            val temp = os.createTempDirectory("test")
            val ram = InMemoryFileSystem.init()
            var contentIndex = 0
            for p <- paths.l do
                val content = contents(contentIndex)
                contentIndex += 1
                contentIndex = contentIndex % contents.size
                val osPath = temp / p
                val ramPath = Paths.get("/") / p
                os.writeContent(osPath, content.s)
                ram.writeContent(ramPath, content.s)
            val diff = DirectoryDiffer(os, ram).diff(temp, Paths.get("/"))
            diff isEqualTo DirectoryDiff.SAME

end FileSystemSuite
