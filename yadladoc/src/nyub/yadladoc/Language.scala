package nyub.yadladoc

case class Language private (val name: String)
object Language:
    val JAVA: Language = Language("java")
    val MARKDOWN: Language = Language("markdown")
    val PYTHON: Language = Language("python")
    val SCALA: Language = Language("scala")
    def named(name: String): Language = Language(name.toLowerCase())
