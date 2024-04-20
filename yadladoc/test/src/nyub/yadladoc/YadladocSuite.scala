package nyub.yadladoc

import java.nio.file.{Files, Path, StandardOpenOption}

class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    testWithinYDocContext("One snippet"): (outputDir, configDir, workingDir) =>
        val markdownFile = makeFile(workingDir, "README.md"):
            l"""
            # One snippet
            ```java ydoc.example.one
            class HelloYadladoc { }
            ```
            """

        Yadladoc(Yadladoc.Settings(outputDir, configDir)).run(markdownFile)

        outputDir.resolve("one.java") hasContent l"""
            package com.example
            class HelloYadladoc { }
            """

    testWithinYDocContext("Three snippets, first two in the same example"):
        (outputDir: Path, configDir: Path, workingDir) =>
            val markdownFile = makeFile(workingDir, "README.md"):
                l"""
            Create a list with listOf(...)
            ```kotlin ydoc.example.kotlin-list-example
            val myList = listOf(1, 2, 3)
            ```
            Retrieve items with get(...)
            ```kotlin ydoc.example.kotlin-list-example
            myList.get(0) // 1
            myList[0] // operator alternative to get()
            ```
            Here is how to define a class:
            ```kotlin ydoc.example.kotlin-class-example
            class SoCool(val coolnessLevel: Int)
            ```
            """

            Yadladoc(Yadladoc.Settings(outputDir, configDir)).run(markdownFile)

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

    def testWithinYDocContext(name: String)(f: (Path, Path, Path) => Any) =
        val withYdocContext =
            FunFixture.map3(withTempDir, withTempDir, withTempDir)
        withYdocContext.test(name): (outputDir, configDir, workingDir) =>
            makeFile(configDir, "kotlin.template")(TestContext.kotlinTemplate)
            makeFile(configDir, "java.template")(TestContext.javaTemplate)
            f(outputDir, configDir, workingDir)

    object TestContext:
        val kotlinTemplate = l"""
            package com.example
            fun main() {
            $${{ydoc.snippet}}
            }
            """

        val javaTemplate = l"""
            package com.example
            $${{ydoc.snippet}}
            """

end YadladocSuite
