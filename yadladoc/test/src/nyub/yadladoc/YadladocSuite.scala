package nyub.yadladoc

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import nyub.yadladoc.filesystem./

class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    testWithinYDocContext("One snippet"): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            # One snippet
            ```java ydoc.example=one
            class HelloYadladoc { }
            ```
            """

        Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
            .run(outputDir, markdownFile)

        outputDir.resolve("one.java") hasContent l"""
            package com.example;
            class HelloYadladoc { }
            """

    testWithinYDocContext("Three snippets, first two in the same example"):
        (outputDir: Path, configDir: Path, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            Create a list with listOf(...)
            ```kotlin ydoc.example=kotlin-list-example
                val myList = listOf(1, 2, 3)
            ```
            Retrieve items with get(...)
            ```kotlin ydoc.example=kotlin-list-example
                myList.get(0) // 1
                myList[0] // operator alternative to get()
            ```
            Here is how to define a class:
            ```kotlin ydoc.example=kotlin-class-example
                class SoCool(val coolnessLevel: Int)
            ```
            """

            val generatedFiles =
                Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
                    .run(outputDir, markdownFile)

            generatedFiles.toList isEqualTo List(
              GeneratedFile(
                Paths.get("kotlin-list-example.kotlin"),
                markdownFile
              ),
              GeneratedFile(
                Paths.get("kotlin-class-example.kotlin"),
                markdownFile
              )
            )

            outputDir.resolve("kotlin-list-example.kotlin") hasContent l"""
            package com.example
            fun main() {
                val myList = listOf(1, 2, 3)
                myList.get(0) // 1
                myList[0] // operator alternative to get()
            }
            """

            outputDir.resolve("kotlin-class-example.kotlin") hasContent l"""
            package com.example
            fun main() {
                class SoCool(val coolnessLevel: Int)
            }
            """

    testWithinYDocContext("Prefix and suffix inclusion"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            Here is how to use an awesome library:
            ```java ydoc.example=surround ydoc.prefix=import-prefix ydoc.suffix=footnote-suffix
            new AwesomeClass().doAmazingStuff(); // Wunderbar !
            ```
            """
            Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("surround.java") hasContent l"""
        package com.example;
        import com.awesome.lib.AwesomeClass;
        new AwesomeClass().doAmazingStuff(); // Wunderbar !
        // this file was written by some team at some date for a precise purpose
        """

    testWithinYDocContext("Check ok for no snippet and valid markdown"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            + Juste
                - A simple
                    * Markdown
            """
            Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
                .check(outputDir, markdownFile) isEqualTo List.empty

    testWithinYDocContext(
      "Check ok if generated files are already there and matching"
    ): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            ```java ydoc.example=ok
            println("Hello world");
            ```
            """
        makeFile(outputDir, "ok.java"):
            l"""
            package com.example;
            println("Hello world");
            """

        Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
            .check(outputDir, markdownFile) isEqualTo List.empty

    testWithinYDocContext(
      "Report errors if a generated file is missing"
    ): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            ```java ydoc.example=ko
            System.out.println("Oooops !");
            ```
            """
        makeFile(outputDir, "notTheOneYouExpected.java", "Some content")

        Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
            .check(outputDir, markdownFile) containsExactlyInAnyOrder List(
          CheckErrors.MissingFile(p"ko.java")
        )

    testWithinYDocContext("Sanitized example name available in template"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            ```txt ydoc.example=a/b
            Test
            ```
            """
            makeFile(
              configDir / "includes",
              "txt.template",
              "${{ydoc.exampleName}}"
            )
            Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("a/b.txt") hasContent "a_b"

    testWithinYDocContext("Indexed example part name available in template"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            ```java ydoc.example=javaExample ydoc.prefix=A
            class A {}
            ```
            ```java ydoc.example=javaExample ydoc.prefix=B
            class B {}
            ```
            """
            makeFile(
              configDir / "includes",
              "A.template",
              "// ${{ydoc.subExampleName}}"
            )
            makeFile(
              configDir / "includes",
              "B.template",
              "// ${{ydoc.subExampleName}}"
            )
            Yadladoc(Yadladoc.ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("javaExample.java") hasContent l"""
            package com.example;
            // javaExample_0
            class A {}
            // javaExample_1
            class B {}
            """

    def testWithinYDocContext(name: String)(f: (Path, Path, Path) => Any) =
        val withYdocContext =
            FunFixture.map3(withTempDir, withTempDir, withTempDir)
        withYdocContext.test(name): (outputDir, configDir, workingDir) =>
            makeFile(configDir / "includes", "kotlin.template")(
              TestContext.kotlinTemplate
            )
            makeFile(configDir / "includes", "java.template")(
              TestContext.javaTemplate
            )
            makeFile(configDir / "includes", "import-prefix.template")(
              TestContext.importPrefixTemplate
            )
            makeFile(configDir / "includes", "footnote-suffix.template")(
              TestContext.footNoteSuffixTemplate
            )
            f(outputDir, configDir, workingDir)

    object TestContext:
        val kotlinTemplate = l"""
            package com.example
            fun main() {
            $${{ydoc.snippet}}
            }
            """

        val javaTemplate = l"""
            package com.example;
            $${{ydoc.snippet}}
            """

        val importPrefixTemplate = l"""
            import com.awesome.lib.AwesomeClass;
        """

        val footNoteSuffixTemplate = l"""
        // this file was written by some team at some date for a precise purpose
        """

end YadladocSuite
