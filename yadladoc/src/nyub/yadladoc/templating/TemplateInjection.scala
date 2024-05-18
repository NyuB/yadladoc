package nyub.yadladoc.templating

trait TemplateInjection:
    def inject(line: String, properties: Map[String, String]): String
    def inject(
        lines: Iterable[String],
        properties: Map[String, String]
    ): Iterable[String] = lines.map(inject(_, properties))
