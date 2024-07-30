package nyub.yadladoc

import java.nio.file.Path
import nyub.filesystem./
import java.nio.file.Paths
import nyub.filesystem.FileSystem
import nyub.filesystem.OsFileSystem
import nyub.interpreter.ScriptDecorator
import nyub.interpreter.ScriptDecoratorService
import nyub.interpreter.CramDecoratorService
import nyub.interpreter.JShellDecoratorService

val DEFAULT_LANGUAGE = Language.named("default")
trait Configuration:
    def properties: Properties
    def configDir: Path
    def includesDir: Path =
        properties.getPathOrDefault("ydoc.includesDir")(configDir / "includes")

    def snippetInjectionKey: String = "ydoc.snippet"

    def exampleNameInjectionKey: String = "ydoc.exampleName"

    def subExampleNameInjectionKey: String = "ydoc.subExampleName"

    def exampleNamePropertyKey: String = "ydoc.example"

    def decoratorIdPropertyKey: String = "ydoc.decorator"

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

    def documentationKindForSnippet(snippet: Snippet): DocumentationKind =
        snippet.properties
            .get(exampleNamePropertyKey)
            .filterNot(_.isBlank)
            .map(DocumentationKind.ExampleSnippet(_, snippet))
            .getOrElse:
                snippet.properties
                    .get(decoratorIdPropertyKey)
                    .filterNot(_.isBlank)
                    .map(DocumentationKind.DecoratedSnippet(_))
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

    def scriptDecorator(
        decoratorId: String,
        snippetProperties: Properties
    ): Option[ScriptDecorator] =
        decoratorServices
            .get(decoratorId)
            .map(
              _.createDecorator(
                properties.extendedWith(snippetProperties).toMap
              )
            )

    def decoratorServices: Map[String, ScriptDecoratorService] =
        Seq(CramDecoratorService(), JShellDecoratorService())
            .map(d => d.id -> d)
            .toMap

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
