package nyub.yadladoc

case class Example(
    val name: String,
    val language: Option[Language],
    val content: Iterable[ExampleContent]
):
    def merge(other: Example): Example =
        if other.language != language then
            throw IllegalArgumentException(
              s"Error trying to merge snippets with different languages ${language} and ${other.language}"
            )
        else
            Example(
              name,
              language,
              content ++ other.content
            )

case class ExampleContent(
    prefixTemplateNames: Iterable[String],
    body: Iterable[String],
    suffixTemplateNames: Iterable[String]
)
