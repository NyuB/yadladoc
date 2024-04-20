package nyub.yadladoc

import java.nio.file.{Files, Path, StandardOpenOption}

class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    withTempDir.test("One snippet"): tmpDir =>
        val markdownFile = makeFile(tmpDir, "README.md"):
            l"""
            # One snippet
            ```java ydoc.example.one
            class HelloYadladoc { }
            ```
            """

        val templateFile = makeFile(tmpDir, "ydoc.template"):
            l"""
            package com.example
            $${{ydoc.snippet}}
            """

        Yadladoc(Yadladoc.Settings(tmpDir, templateFile)).run(markdownFile)

        tmpDir.resolve("one.java") hasContent l"""
            package com.example
            class HelloYadladoc { }
            """

    withTempDir.test("Three snippets, first two in the same example"):
        (tmpDir: Path) =>
            val markdownFile = makeFile(tmpDir, "README.md"):
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
            val templateFile = makeFile(tmpDir, "template.kt"):
                l"""
            package com.example
            fun main() {
            $${{ydoc.snippet}}
            }
            """

            Yadladoc(Yadladoc.Settings(tmpDir, templateFile)).run(markdownFile)

            tmpDir.resolve("kotlin-list-example.kotlin") hasContent l"""
            package com.example
            fun main() {
            val myList = listOf(1, 2, 3)
            myList.get(0) // 1
            myList[0] // operator alternative to get()
            }
            """
            
            tmpDir.resolve("kotlin-class-example.kotlin") hasContent l"""
            package com.example
            fun main() {
            class SoCool(val coolnessLevel: Int)
            }
            """

end YadladocSuite
