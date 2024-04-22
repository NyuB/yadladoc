package nyub.yadladoc

import java.nio.file.Path

trait FileSystem:
    def content(path: Path): String
    def useLines[T](path: Path)(f: Iterable[String] => T): T
    def writeContent(path: Path, content: String): Unit
    def createTempDirectory(prefix: String): Path
