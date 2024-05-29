package nyub.yadladoc

import java.nio.file.Path
import nyub.filesystem./
import java.nio.file.Paths
import nyub.filesystem.FileSystem
import nyub.filesystem.OsFileSystem

val DEFAULT_LANGUAGE = Language.named("default")
trait Configuration:
    def properties: Properties
    def configDir: Path
    def includesDir: Path = properties.getPathOrDefault("ydoc.includesDir")(
      configDir / "includes"
    )

    def templateFile(templateId: TemplateId): Path =
        includesDir / s"${templateId}.template"

    def templateId(
        language: Option[Language],
        properties: Properties
    ): TemplateId =
        val id = properties.getOrDefault("ydoc.template")(
          language.getOrElse(DEFAULT_LANGUAGE).name
        )
        TemplateId(id)

    def templateInjectionPrefix =
        properties.getOrDefault("ydoc.templateInjectionPrefix")("${{")

    def templateInjectionPostfix =
        properties.getOrDefault("ydoc.templateInjectionPostfix")("}}")

    def snippetInjectionKey: String =
        properties.getOrDefault("ydoc.snippetInjectionKey")("ydoc.snippet")

    def exampleNameInjectionKey: String =
        properties.getOrDefault("ydoc.exampleNameInjectionKey")(
          "ydoc.exampleName"
        )

    def exampleNamePropertyKey: String = properties.getOrDefault(
      "ydoc.exampleNamePropertyKey"
    )("ydoc.example")

    def subExampleNameInjectionKey: String = properties.getOrDefault(
      "ydoc.subExampleNamePropertyKey"
    )("ydoc.subExampleName")

    def exampleForSnippet(snippet: Snippet): DocumentationKind =
        snippet.properties
            .get(exampleNamePropertyKey)
            .filterNot(_.isBlank)
            .map(DocumentationKind.ExampleSnippet(_, snippet))
            .getOrElse(DocumentationKind.Raw)

    def exampleFile(
        exampleName: String,
        exampleLanguage: Option[Language]
    ): Path =
        Paths.get(
          s"${exampleName}"
        )

    def prefixTemplateIds(
        snippet: Snippet
    ): Iterable[TemplateId] =
        snippet.properties.get("ydoc.prefix").toList.map(TemplateId(_))

    def suffixTemplateIds(
        snippet: Snippet
    ): Iterable[TemplateId] =
        snippet.properties.get("ydoc.suffix").toList.map(TemplateId(_))

case class ConfigurationFromFile(
    override val configDir: Path,
    private val storage: FileSystem = OsFileSystem()
) extends Configuration:
    override val properties: Properties =
        val propertyFile = configDir / "ydoc.properties"
        if !propertyFile.toFile().isFile() then Properties.empty
        else
            storage.useLines(propertyFile): lines =>
                lines.foldLeft(Properties.empty): (props, line) =>
                    props.extendedWith(Properties.ofLine(line))
