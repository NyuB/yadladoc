package nyub.yadladoc

import java.nio.file.Path
import munit.diff.Diffs

case class Results[A](
    val results: A,
    val errors: Seq[Errors]
):

    def map[B](f: A => B): Results[B] =
        Results(f(results), errors)

    def flatMap[B](f: A => Results[B]): Results[B] =
        val nextResults = f(results)
        Results(nextResults.results, errors ++ nextResults.errors)

    def aggregate[B, C](other: Results[B])(
        f: (A, B) => Results[C]
    ): Results[C] =
        val nextResults = f(results, other.results)
        Results(
          nextResults.results,
          errors ++ other.errors ++ nextResults.errors
        )

object Results:
    def success[A](results: A): Results[A] = Results(results, Seq.empty)

sealed trait Errors:
    def prettyPrintedMessage: String

/** Errors spawned when comparing the actual file system to what would have have
  * been generated by a [[Yadladoc]] run
  */
sealed trait CheckErrors extends Errors

object CheckErrors:
    /** A file would have been generated but is not present in actual file
      * system
      *
      * @param generatedFileName
      *   path to the file that would have been generated
      */
    case class MissingFile(generatedFileName: Path) extends Errors:
        override def prettyPrintedMessage: String =
            s"File '${generatedFileName}' is missing"

    /** A generated file would differs from its counterpart in the actual file
      * system
      *
      * @param fileName
      *   generated file path
      * @param actualContent
      *   content in actual file system
      * @param expectedContent
      *   content that would have been generated
      */
    case class MismatchingContent(
        fileName: Path,
        actualContent: String,
        expectedContent: String
    ) extends Errors:
        override def prettyPrintedMessage: String =
            val diff = Diffs.create(actualContent, expectedContent)
            s"""File '${fileName}' has mismatching content with what would have been generated
${diff.unifiedDiff}"""

/** A file generated from a documentation example
  *
  * @param parent
  *   optional root of generated file path
  * @param relative
  *   generated file path relative to `parent` or absolute if `parent` is `None`
  * @param from
  *   path pointing to the documentation file this [[GeneratedFile]] was
  *   generated from
  */
case class GeneratedFile(
    private val parent: Option[Path],
    private val relative: Path,
    val from: Path
):
    /** @return
      *   full path to the generated file
      */
    def full: Path = parent.map(_.resolve(relative)).getOrElse(relative)

    /** @return
      *   short version of the generated file from which `parent` root may have
      *   been stripped
      */
    def short: Path = relative
