package nyub.yadladoc

import java.nio.file.{Files, Path, StandardOpenOption}

class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    withTempDir.test("One snippet"): (tmpDir: Path) =>
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

end YadladocSuite
