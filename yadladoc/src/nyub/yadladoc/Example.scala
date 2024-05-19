package nyub.yadladoc

import nyub.templating.TemplateInjection

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

    def build(
        templating: TemplateInjection,
        getTemplate: TemplateId => Iterable[String],
        bodyInjectionKey: String,
        exampleNameInjectionKey: String,
        partNameInjectionKey: String,
        additionalInjections: Map[String, String]
    ): Iterable[String] =
        val injectionProperties: Map[String, String] = additionalInjections
            .updated(exampleNameInjectionKey, sanitizedName(name))
        val all = content.zipWithIndex.flatMap: (c, i) =>

            val partProperties = injectionProperties.updated(
              partNameInjectionKey,
              sanitizedName(name + "_" + i)
            )
            val prefixLines = c.prefixTemplateIds
                .flatMap(getTemplate)
                .map(templating.inject(_, partProperties))

            val suffixLines = c.suffixTemplateIds
                .flatMap(getTemplate)
                .map(templating.inject(_, partProperties))

            prefixLines ++ c.body ++ suffixLines

        templating.inject(
          getTemplate(template),
          injectionProperties
              .updated(bodyInjectionKey, all.mkString("\n"))
        )

    private def sanitizedName(exampleName: String): String =
        exampleName.replaceAll("[/\\:.]+", "_")

case class ExampleContent(
    prefixTemplateIds: Iterable[TemplateId],
    body: Iterable[String],
    suffixTemplateIds: Iterable[TemplateId]
)
