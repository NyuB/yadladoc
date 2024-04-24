package nyub.yadladoc.templating

class SurroundingTemplateInjection() extends TemplateInjection:
    override def inject(line: String, properties: Map[String, String]): String =
        Raw("", properties).replace(line)

    private trait Replace:
        def replace(line: String): String

    private class Raw(previous: String, properties: Map[String, String])
        extends Replace:
        override def replace(line: String): String =
            var index = 0
            while index < line.length && !line
                    .substring(index)
                    .startsWith(prefix)
            do index += 1
            if index == line.length then previous + line
            else
                Property(previous + line.substring(0, index), "", properties)
                    .replace(line.substring(index + prefix.length))

    private class Property(
        previous: String,
        propertyName: String,
        properties: Map[String, String]
    ) extends Replace:
        override def replace(line: String): String =
            if line.length < postfix.length then
                previous + prefix + propertyName + line
            else if line.startsWith(postfix) then
                val propertyValue =
                    properties.get(propertyName).getOrElse(defaultPropertyValue)
                Raw(previous + propertyValue, properties).replace(
                  line.substring(postfix.length)
                )
            else
                Property(previous, propertyName + line(0), properties)
                    .replace(line.substring(1))

    private val prefix = "${{"
    private val postfix = "}}"
    private val defaultPropertyValue = ""
