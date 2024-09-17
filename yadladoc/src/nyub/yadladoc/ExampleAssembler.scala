package nyub.yadladoc

import nyub.templating.TemplateInjection

class ExampleAssembler(
    private val templating: TemplateInjection,
    private val getTemplate: TemplateId => Iterable[String],
    private val bodyInjectionKey: String,
    private val exampleNameInjectionKey: String,
    private val partNameInjectionKey: String,
    private val additionalInjections: Map[String, String]
):
    def assemble(
        example: Example
    ): Iterable[String] =
        val injectionProperties: Map[String, String] = additionalInjections
            .updated(exampleNameInjectionKey, sanitizedName(example.name))
        val all = example.content.zipWithIndex.flatMap: (c, i) =>

            val partProperties = injectionProperties.updated(
              partNameInjectionKey,
              sanitizedName(example.name + "_" + i)
            )
            val prefixLines = c.prefixTemplateIds
                .flatMap(getTemplate)
                .map(templating.inject(_, partProperties))

            val suffixLines = c.suffixTemplateIds
                .flatMap(getTemplate)
                .map(templating.inject(_, partProperties))

            prefixLines ++ c.body ++ suffixLines

        templating.inject(
          getTemplate(example.template),
          injectionProperties
              .updated(bodyInjectionKey, all.mkString("\n"))
        )

    private def sanitizedName(exampleName: String): String =
        exampleName.replaceAll("[/\\:.]+", "_")
