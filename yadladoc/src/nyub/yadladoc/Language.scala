package nyub.yadladoc

case class Language private (val name: String)
object Language:
    val JAVA = Language("java")
    val MARKDOWN = Language("markdown")
    val PYTHON = Language("python")
    val SCALA = Language("scala")
    def named(name: String): Language = Language(name.toLowerCase())
