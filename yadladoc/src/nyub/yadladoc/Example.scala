package nyub.yadladoc

case class Example(
    val name: String,
    val template: TemplateId,
    val language: Option[Language],
    val content: Iterable[ExampleContent]
):
    def merge(other: Example): Example =
        require(other.language == this.language)
        require(other.name == this.name)
        require(other.template == this.template)

        Example(
          name,
          template,
          language,
          content ++ other.content
        )

case class ExampleContent(
    prefixTemplateIds: Iterable[TemplateId],
    body: Iterable[String],
    suffixTemplateIds: Iterable[TemplateId]
)
