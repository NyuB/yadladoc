package nyub.yadladoc

import java.nio.file.Path
import nyub.filesystem./
import java.nio.file.Paths
import nyub.filesystem.FileSystem
import nyub.filesystem.OsFileSystem
import nyub.interpreter.ScriptDecoratorService
import java.util.ServiceLoader
import nyub.interpreter.ScriptDecorator
import nyub.interpreter.CramDecoratorService

val DEFAULT_LANGUAGE = Language.named("default")
trait Configuration:
    def properties: Properties
    def configDir: Path
    def constants: ConfigurationConstants

    def includesDir: Path =
        properties.getPathOrDefault(constants.includeDirPropertyKey)(
          configDir / "includes"
        )

    def templateFile(templateId: TemplateId): Path =
        includesDir / s"${templateId}.template"

    def templateId(
        language: Option[Language],
        properties: Properties
    ): TemplateId =
        val id = properties.getOrDefault(constants.templateIdPropertyKey)(
          language.getOrElse(DEFAULT_LANGUAGE).name
        )
        TemplateId(id)

    def templateInjectionPrefix =
        properties.getOrDefault(
          constants.templateConstants.injectionPrefixPropertyKey
        )(constants.templateConstants.defaultInjectionPrefix)

    def templateInjectionPostfix =
        properties.getOrDefault(
          constants.templateConstants.injectionSuffixPropertyKey
        )(constants.templateConstants.defaultInjectionSuffix)

    def documentationKindForSnippet(snippet: Snippet): DocumentationKind =
        snippet.properties
            .get(constants.exampleNamePropertyKey)
            .filterNot(_.isBlank)
            .map(DocumentationKind.ExampleSnippet(_, snippet))
            .getOrElse:
                snippet.properties
                    .get(constants.decoratorIdPropertyKey)
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
        snippet.properties
            .get(constants.templateConstants.prefixTemplatePropertyKey)
            .toList
            .map(TemplateId(_))

    def suffixTemplateIds(
        snippet: Snippet
    ): Iterable[TemplateId] =
        snippet.properties
            .get(constants.templateConstants.suffixTemplatePropertyKey)
            .toList
            .map(TemplateId(_))

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

    final def decoratorServices: Map[String, ScriptDecoratorService] =
        val mutableMap =
            scala.collection.mutable.Map.from(builtinDecoratorServices)
        ServiceLoader
            .load(classOf[ScriptDecoratorService])
            .forEach(s => mutableMap(s.id) = s)
        mutableMap.toMap

    /** **Please note**: this method should only return decorator services meant
      * to be usable within any user environment
      *
      *   - Do not add any JVM reflective feature as yadladoc could be packaged
      *     as native binaries
      *   - Do not add any OS specific feature as yadladoc supports both Unix
      *     and Windows
      *
      * For more specific or user-defined decorators, use SPI embedded in the
      * classpath
      * @return
      */
    def builtinDecoratorServices: Map[String, ScriptDecoratorService] =
        Map("cram" -> CramDecoratorService())

/** Constants meant to be overridable for programatic use (i.e. as a library) of
  * yadladoc
  *
  * For user-provided constants use [[nyub.yadladoc.Properties]]
  */
trait ConfigurationConstants:
    def snippetInjectionKey: String

    def exampleNameInjectionKey: String

    def subExampleNameInjectionKey: String

    def exampleNamePropertyKey: String

    def decoratorIdPropertyKey: String

    def templateIdPropertyKey: String

    def includeDirPropertyKey: String

    def templateConstants: TemplateConstants

    /** Constants related to yadladoc's templating features
      */
    trait TemplateConstants:
        /** @return
          *   the property to look for in an example properties when searching a
          *   for the template to insert before this example
          */
        def prefixTemplatePropertyKey: String

        /** @return
          *   the property to look for in an example properties when searching a
          *   for the template to insert after this example
          */
        def suffixTemplatePropertyKey: String

        /** @return
          *   the character sequence marking the start of an injection key in a
          *   template
          */
        def injectionPrefixPropertyKey: String

        /** @return
          *   the character sequence marking the end of an injection key in a
          *   template
          */
        def injectionSuffixPropertyKey: String

        /** @return
          *   the default character sequence to use when
          *   [[TemplateConstants#injectionPrefixPropertyKey]] is not found
          */
        def defaultInjectionPrefix: String

        /** @return
          *   the default character sequence to use when
          *   [[TemplateConstants#injectionSuffixPropertyKey]] is not found
          */
        def defaultInjectionSuffix: String

object ConfigurationConstants:
    object DEFAULTS extends ConfigurationConstants:
        override val snippetInjectionKey: String = "ydoc.snippet"

        override val exampleNameInjectionKey: String = "ydoc.exampleName"

        override val subExampleNameInjectionKey: String = "ydoc.subExampleName"

        override val exampleNamePropertyKey: String = "ydoc.example"

        override val decoratorIdPropertyKey: String = "ydoc.decorator"

        override val templateIdPropertyKey: String = "ydoc.template"

        override val includeDirPropertyKey: String = "ydoc.includeDir"

        override val templateConstants: TemplateConstants = new:
            override val prefixTemplatePropertyKey: String = "ydoc.prefix"
            override val suffixTemplatePropertyKey: String = "ydoc.suffix"

            override val injectionPrefixPropertyKey: String =
                "ydoc.templateInjectionPrefix"

            override val injectionSuffixPropertyKey: String =
                "ydoc.templateInjectionPostfix"

            override val defaultInjectionPrefix: String = "${{"
            override val defaultInjectionSuffix: String = "}}"

case class ConfigurationFromFile(
    override val configDir: Path,
    override val constants: ConfigurationConstants,
    private val storage: FileSystem = OsFileSystem()
) extends Configuration:
    override val properties: Properties =
        val propertyFile = configDir / "ydoc.properties"
        if !propertyFile.toFile().isFile() then Properties.empty
        else
            storage.useLines(propertyFile): lines =>
                lines.foldLeft(Properties.empty): (props, line) =>
                    props.extendedWith(Properties.ofLine(line))
