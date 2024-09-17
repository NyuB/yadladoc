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
