package nyub.yadladoc

import java.nio.file.Path

sealed trait Errors:
    def prettyPrintedMessage: String

object CheckErrors:
    case class MissingFile(generatedFileName: Path) extends Errors:
        override def prettyPrintedMessage: String =
            s"File '${generatedFileName}' is missing"

    case class UnexpectedFile(fileName: Path) extends Errors:
        override def prettyPrintedMessage: String =
            s"File '${fileName}' was not expected"

    case class MismatchingContent(
        fileName: Path,
        actualContent: String,
        expectedContent: String
    ) extends Errors:
        override def prettyPrintedMessage: String =
            s"""File '${fileName}' has mismatching content with what would have been generated
Expected
vvvvvvv
${actualContent}
^^^^^^^

Actual
vvvvvvv
${actualContent}
^^^^^^^"""

case class GeneratedFile(val file: Path, val from: Path)
