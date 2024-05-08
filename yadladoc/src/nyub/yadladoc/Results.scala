package nyub.yadladoc

import java.nio.file.Path

sealed trait Errors
object CheckErrors:
    case class MissingFile(generatedFileName: Path) extends Errors
    case class UnexpectedFile(fileName: Path) extends Errors
    case class MismatchingContent(
        fileName: Path,
        actualContent: String,
        expectedContent: String
    ) extends Errors

case class GeneratedFile(val file: Path, val from: Path)
