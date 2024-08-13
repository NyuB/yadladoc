package nyub.yadladoc

import nyub.assert.AssertExtensions
import java.nio.file.Files
import nyub.interpreter.CramDecoratorService
import nyub.interpreter.JShellDecoratorService

class ConfigurationSuite extends munit.FunSuite with AssertExtensions:
    test("built-in services"):
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
