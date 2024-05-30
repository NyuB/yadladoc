package nyub.yadladoc

import java.nio.file.Path
import nyub.assert.AssertExtensions

import nyub.filesystem./

@annotation.nowarn("msg=unused value") // ignore generated files' paths
class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    testWithinYDocContext("One snippet"): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            # One snippet
            ```java ydoc.example=one.java
            class HelloYadladoc { }
            ```
            """

        Yadladoc(ConfigurationFromFile(configDir))
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
            ```kotlin ydoc.example=kotlin-list-example.kt
                val myList = listOf(1, 2, 3)
            ```
            Retrieve items with get(...)
            ```kotlin ydoc.example=kotlin-list-example.kt
                myList.get(0) // 1
                myList[0] // operator alternative to get()
            ```
            Here is how to define a class:
            ```kotlin ydoc.example=kotlin-class-example.kt
                class SoCool(val coolnessLevel: Int)
            ```
            """

            val generatedFiles =
                Yadladoc(ConfigurationFromFile(configDir))
                    .run(outputDir, markdownFile)

            generatedFiles.toList isEqualTo List(
              GeneratedFile(
                Some(outputDir),
                p"kotlin-list-example.kt",
                markdownFile
              ),
              GeneratedFile(
                Some(outputDir),
                p"kotlin-class-example.kt",
                markdownFile
              )
            )

            outputDir.resolve("kotlin-list-example.kt") hasContent l"""
            package com.example
            fun main() {
                val myList = listOf(1, 2, 3)
                myList.get(0) // 1
                myList[0] // operator alternative to get()
            }
            """

            outputDir.resolve("kotlin-class-example.kt") hasContent l"""
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
            ```java ydoc.example=surround.java ydoc.prefix=import-prefix ydoc.suffix=footnote-suffix
            new AwesomeClass().doAmazingStuff(); // Wunderbar !
            ```
            """
            Yadladoc(ConfigurationFromFile(configDir))
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
            Yadladoc(ConfigurationFromFile(configDir))
                .check(outputDir, markdownFile) isEqualTo List.empty

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

        Yadladoc(ConfigurationFromFile(configDir))
            .check(outputDir, markdownFile) isEqualTo List.empty

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

        Yadladoc(ConfigurationFromFile(configDir))
            .check(outputDir, markdownFile) containsExactlyInAnyOrder List(
          CheckErrors.MissingFile(p"ko.java")
        )

    testWithinYDocContext("Sanitized example name available in template"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            ```txt ydoc.example=a/b.txt
            Test
            ```
            """
            makeFile(
              configDir / "includes",
              "txt.template",
              "${{ydoc.exampleName}}"
            )
            Yadladoc(ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("a/b.txt") hasContent "a_b_txt"

    testWithinYDocContext("Indexed example part name available in template"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            ```java ydoc.example=javaExample.java ydoc.prefix=A
            class A {}
            ```
            ```java ydoc.example=javaExample.java ydoc.prefix=B
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
            Yadladoc(ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve("javaExample.java") hasContent l"""
            package com.example;
            // javaExample_java_0
            class A {}
            // javaExample_java_1
            class B {}
            """

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
            Yadladoc(ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)

            outputDir.resolve(
              "javaExample.java"
            ) hasContent """class Custom { String s = "Hello"; }"""

    testWithinYDocContext("In-place markdown decoration"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
                ```java ydoc.interpreter=jshell
                java.util.List.of(1,2,3)
                ```
                """
            Yadladoc(ConfigurationFromFile(configDir))
                .run(outputDir, markdownFile)
            markdownFile hasContent l"""
            ```java ydoc.interpreter=jshell
            java.util.List.of(1,2,3)
            //> [1, 2, 3]
            ```
            """

    testWithinYDocContext("Check modified markdown file"):
        (outputDir, configDir, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
                ```java ydoc.interpreter=jshell
                java.util.List.of(1,2,3)
                ```
                """
            Yadladoc(ConfigurationFromFile(configDir))
                .check(outputDir, markdownFile) match
                case List(CheckErrors.MismatchingContent(f, _, _))
                    if markdownFile == f =>
                    ()
                case _ =>
                    fail(
                      "Expected content mismatch error for decorated markdown file"
                    )

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
