package nyub.yadladoc

import java.nio.file.Path

trait StorageAccess:
    def content[T](path: Path): String
    def useLines[T](path: Path)(f: Iterable[String] => T): T
    def writeContent(path: Path, content: String): Unit
