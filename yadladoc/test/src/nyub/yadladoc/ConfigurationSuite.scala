package nyub.yadladoc

import nyub.assert.AssertExtensions
import java.nio.file.Files
import nyub.interpreter.CramDecoratorService
import nyub.interpreter.JShellDecoratorService

class ConfigurationSuite extends munit.FunSuite with AssertExtensions:
    test("Documentation kind is example when ydoc.example property is present"):
        TestConfig.documentationKindForSnippet(
          Snippet(None, Seq.empty, Properties("ydoc.example" -> "A"))
        ) matches:
            case DocumentationKind.ExampleSnippet("A", _) => ()

    test(
      "Documentation kind is decorated when ydoc.decorator property is present"
    ):
        TestConfig.documentationKindForSnippet(
          Snippet(
            None,
            Seq.empty,
            Properties("ydoc.decorator" -> "decoratorId")
          )
        ) `is equal to`
            DocumentationKind.DecoratedSnippet("decoratorId")

    test("Documentation kind is raw when no special property is present"):
        TestConfig.documentationKindForSnippet(
          Snippet(None, Seq.empty, Properties())
        ) `is equal to` DocumentationKind.Raw

    test("Templates are searched in includes dir and suffixed by '.template'"):
        TestConfig.templateFile(
          TemplateId("id")
        ) `is equal to` TestConfig.configDir
            .resolve("includes")
            .resolve("id.template")

    test(
      "Given no language and no template property, default template is used"
    ):
        TestConfig.templateId(None, Properties.empty) `is equal to` TemplateId(
          "default"
        )

    test(
      "Given a language and no template property, language name is used as template id"
    ):
        TestConfig.templateId(
          Some(Language.named("scala")),
          Properties.empty
        ) `is equal to` TemplateId("scala")

    test(
      "Given a language and a template property, the property is used as template id"
    ):
        TestConfig.templateId(
          Some(Language.named("scala")),
          Properties(TestConfig.constants.templateIdPropertyKey -> "prop")
        ) `is equal to` TemplateId("prop")

    test(
      "Given no language and a template property, the property is used as template id"
    ):
        TestConfig.templateId(
          None,
          Properties(TestConfig.constants.templateIdPropertyKey -> "prop")
        ) `is equal to` TemplateId("prop")

    test("Built-in services"):
        TestConfig.decoratorServices
            .get("cram")
            .map(_.getClass()) `is equal to some` classOf[CramDecoratorService]

        TestConfig.decoratorServices
            .get("jshell")
            .map(_.getClass()) `is equal to some` classOf[
          JShellDecoratorService
        ]

    object TestConfig extends Configuration:
        override val configDir = Files.createTempDirectory("test")
        override val properties = Properties.ofMap(Map.empty)

        override val constants = ConfigurationConstants.DEFAULTS

end ConfigurationSuite
