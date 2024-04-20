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
            ```java
            class HelloYadladoc { }
            ```
            """

        val templateFile = makeFile(tmpDir, "ydoc.template"):
            l"""
            package com.example
            $${{ydoc.snippet}}
            """

        Yadladoc(Yadladoc.Settings(tmpDir, templateFile)).run(markdownFile)

        tmpDir.resolve("yadladoc.txt") hasContent l"""
            package com.example
            class HelloYadladoc { }
            """

    withTempDir.test("Three snippets concatenated"): (tmpDir: Path) =>
        val markdownFile = makeFile(tmpDir, "README.md"):
            l"""
            Create a list with listOf(...)
            ```kotlin
            val myList = listOf(1, 2, 3)
            ```
            Retrieve items with get(...)
            ```kotlin
            myList.get(0) // 1
            myList[0] // operator alternative to get()
            ```
            Check if an object is in the list with contains(...)
            ```kotlin
            myList.contains(2) // true
            myList.contains(42) // false
            42 in myList // operator alternative to contains
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

        tmpDir.resolve("yadladoc.txt") hasContent l"""
            package com.example
            fun main() {
            val myList = listOf(1, 2, 3)
            myList.get(0) // 1
            myList[0] // operator alternative to get()
            myList.contains(2) // true
            myList.contains(42) // false
            42 in myList // operator alternative to contains
            }
            """

end YadladocSuite
