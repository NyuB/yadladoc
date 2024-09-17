package nyub.yadladoc

import java.nio.file.Path
import nyub.assert.AssertExtensions

import nyub.filesystem./

@annotation.nowarn("msg=unused value") // ignore generated files' paths
class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    testWithinYDocContext("Generate one file from one snippet"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            # One snippet
            ```java ydoc.example=one.java
            class HelloYadladoc { }
            ```
            """

            Yadladoc(testConfig(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("one.java") `has content` l"""
            package com.example;
            class HelloYadladoc { }
            """

    testWithinYDocContext("Generate files from multiple snippets"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            # One snippet
            ```java ydoc.example=one.java
            class One { }
            ```
            # Another snippet
            ```java ydoc.example=two.java
            class Two { }
            ```
            """

            Yadladoc(testConfig(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("one.java") `has content` l"""
            package com.example;
            class One { }
            """
            outputDir.resolve("two.java") `has content` l"""
            package com.example;
            class Two { }
            """

    testWithinYDocContext("Check ok for no snippet and valid markdown"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            + Juste
                - A simple
                    * Markdown
            """
            Yadladoc(testConfig(configDir))
                .check(outputDir, markdownFile) `is equal to` Results(
              Seq.empty,
              Seq.empty
            )

    testWithinYDocContext(
      "Check ok if generated files are already there and matching"
    ): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
                ```java ydoc.example=ok.java
                println("Hello world");
                ```
                """
        makeFile(outputDir, "ok.java"):
            l"""
                package com.example;
                println("Hello world");
                """

        Yadladoc(testConfig(configDir))
            .check(outputDir, markdownFile) matches:
            case Results(
                  Seq(GeneratedFile(Some(_), _, generatedFrom)),
                  Seq()
                ) =>
                generatedFrom `is equal to` markdownFile

    testWithinYDocContext(
      "Report errors if a generated file is missing"
    ): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            ```java ydoc.example=ko.java
            System.out.println("Oooops !");
            ```
            """
        makeFile(outputDir, "notTheOneYouExpected.java", "Some content")

        Yadladoc(testConfig(configDir))
            .check(
              outputDir,
              markdownFile
            )
            .errors `contains exactly in any order` List(
          CheckErrors.MissingFile(p"ko.java")
        )

    testWithinYDocContext("Custom template per example"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            ```java ydoc.example=javaExample.java ydoc.template=custom
            String s = "Hello";
            ```
            """
            makeFile(
              configDir / "includes",
              "custom.template",
              "class Custom { ${{ydoc.snippet}} }"
            )
            Yadladoc(testConfig(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve(
              "javaExample.java"
            ) `has content` """class Custom { String s = "Hello"; }"""

    testWithinYDocContext("In-place markdown decoration"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
                ```java ydoc.decorator=jshell
                java.util.List.of(1,2,3)
                ```
                """
            Yadladoc(testConfig(configDir))
                .run(outputDir, markdownFile)
            markdownFile `has content` l"""
            ```java ydoc.decorator=jshell
            java.util.List.of(1,2,3)
            //> [1, 2, 3]
            ```
            """

    testWithinYDocContext("Check modified markdown file"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
                ```java ydoc.decorator=jshell
                java.util.List.of(1,2,3)
                ```
                """
            Yadladoc(testConfig(configDir))
                .check(outputDir, markdownFile)
                .errors
                .toList matches:
                case List(CheckErrors.MismatchingContent(f, _, _)) =>
                    markdownFile `is equal to` f

    private def testWithinYDocContext(name: String)(
        f: (Path, Path, Path) => Any
    ) =
        val withYdocContext =
            FunFixture.map3(withTempDir, withTempDir, withTempDir)
        withYdocContext.test(name): (outputDir, configDir, workingDir) =>
            makeFile(configDir / "includes", "java.template")(
              javaTemplate
            )
            f(outputDir, configDir, workingDir)

    private def testConfig(configDir: Path): Configuration =
        ConfigurationFromFile(configDir, ConfigurationConstants.DEFAULTS)

    private val javaTemplate = l"""
        package com.example;
        $${{ydoc.snippet}}
        """

end YadladocSuite
