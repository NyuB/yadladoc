package nyub.yadladoc

case class TemplateId(private val id: String):
    override def toString(): String = id
