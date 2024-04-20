package nyub.yadladoc

import java.nio.file.{Files, Path, StandardOpenOption}

class YadladocSuite
    extends munit.FunSuite
    with AssertExtensions
    with SuiteExtensions:

    withTempDir.test("Inject one sample"): (tmpDir: Path) =>
        val markdown = l"""
        # One snippet
        ```java
        class HelloYadladoc { }
        ```
        """
        val markdownFile = inTempDir(tmpDir, "README.md", markdown)

        val template = l"""
        package com.example
        $${{ydoc.snippet}}
        """
        val templateFile = inTempDir(tmpDir, "ydoc.template", template)

        Yadladoc(Yadladoc.Settings(tmpDir, templateFile)).run(markdownFile)
        Files.readString(tmpDir.resolve("yadladoc.txt")) isEqualToLines l"""
        package com.example
        class HelloYadladoc { }
        """

    private def inTempDir(
        tempDir: Path,
        filename: String,
        content: String
    ): Path =
        val res = tempDir.resolve(filename)
        Files.write(res, content.getBytes(), StandardOpenOption.CREATE)
        res

    private def inTempDir(
        tempDir: Path,
        filename: String,
        content: Iterable[String]
    ): Path =
        inTempDir(tempDir, filename, content.mkString("\n"))

end YadladocSuite
