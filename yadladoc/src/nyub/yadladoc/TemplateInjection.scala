package nyub.yadladoc

import scala.languageFeature.postfixOps

class TemplateInjection(properties: Map[String, String]):
    def inject(line: String): String =
        Raw("").replace(line)

    private trait Replace:
        def replace(line: String): String

    private class Raw(previous: String) extends Replace:
        override def replace(line: String): String =
            var index = 0
            while index < line.length && !line
                    .substring(index)
                    .startsWith(prefix)
            do index += 1
            if index == line.length then previous + line
            else
                Property(previous + line.substring(0, index), "")
                    .replace(line.substring(index + prefix.length))

    private class Property(previous: String, propertyName: String)
        extends Replace:
        override def replace(line: String): String =
            if line.length < postfix.length then
                previous + prefix + propertyName + line
            else if line.startsWith(postfix) then
                val propertyValue =
                    properties.get(propertyName).getOrElse(defaultPropertyValue)
                Raw(previous + propertyValue).replace(
                  line.substring(postfix.length)
                )
            else
                Property(previous, propertyName + line(0))
                    .replace(line.substring(1))

    private val prefix = "${{"
    private val postfix = "}}"
    private val defaultPropertyValue = ""
