package nyub.yadladoc

import nyub.markdown.Markdown
import nyub.filesystem.{
    /,
    useLines,
    FileSystem,
    FileTree,
    InMemoryFileSystem,
    OsFileSystem
}
import nyub.templating.{SurroundingTemplateInjection, TemplateInjection}

import java.nio.file.Path

class Yadladoc(
    private val config: Configuration,
    private val fileSystem: FileSystem = OsFileSystem()
):
    private val templating: TemplateInjection = SurroundingTemplateInjection(
      config.templateInjectionPrefix,
      config.templateInjectionPostfix
    )

    def run(outputDir: Path, markdownFile: Path): Iterable[GeneratedFile] =
        run(outputDir, markdownFile, fileSystem)

    def check(outputDir: Path, markdownFile: Path): List[Errors] =
        val checkFs = InMemoryFileSystem.init()
        val checkDir = checkFs.createTempDirectory("check")

        val generated = run(checkDir, markdownFile, checkFs)
        generated
            .map(checkGeneratedFile(_, outputDir, checkFs, checkDir))
            .flatMap(_.toList)
            .toList

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Iterable[GeneratedFile] =
        val examples = markdownFile
            .useLines(lines => dogGenFromMarkdown(Markdown.parse(lines)))
            .exampleSnippets
            .examples

        for example <- examples.values yield
            val fullExample = buildFullExample(example)
            val exampleFile = config.exampleFile(example.name, example.language)

            writeFs.writeContent(
              outputDir / exampleFile,
              fullExample.mkString("\n")
            )

            GeneratedFile(exampleFile, markdownFile)

    private def buildFullExample(example: Example) =
        example.build(
          templating,
          id => config.templateFile(id).useLines(_.toList),
          config.snippetInjectionKey,
          config.exampleNameInjectionKey,
          config.subExampleNameInjectionKey,
          config.properties.toMap
        )

    private def dogGenFromMarkdown(
        markdown: Iterable[Markdown.Block]
    ): DocumentationGeneration =
        markdown.foldLeft(DocumentationGeneration.init(config)): (doc, block) =>
            block match
                case snippet: Markdown.Snippet =>
                    config.exampleForSnippet(snippet.toDocSnippet) match
                        case example: DocumentationKind.ExampleSnippet =>
                            doc.addExampleSnippet(snippet, example)
                        case DocumentationKind.InterpretedSnippet(_) =>
                            doc.addRawSnippet(snippet)
                        case DocumentationKind.Raw =>
                            doc.addRawSnippet(snippet) // no doc to generate
                case raw: Markdown.Raw =>
                    doc.addRawBlock(raw) // no doc to generate

    extension (s: Markdown.Snippet)
        private def toDocSnippet: Snippet =
            Snippet(
              s.header.language,
              s.lines,
              Properties.ofLine(s.header.afterLanguage)
            )

    private def checkGeneratedFile(
        generated: GeneratedFile,
        outputDir: Path,
        checkFs: FileSystem,
        checkDir: Path
    ): Option[Errors] =
        val actualFile = fileSystem.toFileTree(outputDir / generated.file)
        val checkFile = checkFs.toFileTree(checkDir / generated.file)

        actualFile -> checkFile match
            case _ -> None =>
                throw IllegalStateException(
                  "Generated files should exist in check file system"
                )
            case _ -> Some(FileTree.Dir(_)) =>
                throw IllegalStateException(
                  "Generated files should not be a directory in check file system"
                )
            case actual -> Some(FileTree.File(expected)) =>
                actual match
                    case None => Some(CheckErrors.MissingFile(generated.file))
                    case Some(FileTree.Dir(_)) =>
                        Some(CheckErrors.MissingFile(generated.file))
                    case Some(FileTree.File(af)) =>
                        val actualContent = fileSystem.content(af)
                        val expectedContent =
                            checkFs.content(expected)
                        if actualContent == expectedContent then None
                        else
                            Some(
                              CheckErrors.MismatchingContent(
                                generated.file,
                                actualContent,
                                expectedContent
                              )
                            )
