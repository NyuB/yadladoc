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
            .map(checkGeneratedFile(_, outputDir, checkFs))
            .flatMap(_.toList)
            .toList

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Iterable[GeneratedFile] =
        val markdownLines = markdownFile.useLines(_.toSeq)
        val docGen = dogGenFromMarkdown(Markdown.parse(markdownLines))

        val generatedExamples =
            for example <- docGen.exampleSnippets.examples.values yield
                val fullExample = buildFullExample(example)
                val exampleFile =
                    config.exampleFile(example.name, example.language)

                writeFs.writeContent(
                  outputDir / exampleFile,
                  fullExample.mkString("\n")
                )

                GeneratedFile(Some(outputDir), exampleFile, markdownFile)
        val decoratedMarkdownLines = docGen.markdownDecoration.decoratedLines
        if decoratedMarkdownLines == markdownLines then generatedExamples
        else
            generatedExamples.toSeq :+ {
                writeFs.writeContent(
                  markdownFile,
                  decoratedMarkdownLines.mkString("\n")
                )
                GeneratedFile(None, markdownFile, markdownFile)
            }

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
                    config.documentationKindForSnippet(
                      snippet.toDocSnippet
                    ) match
                        case example: DocumentationKind.ExampleSnippet =>
                            doc.addExampleSnippet(snippet, example)
                        case DocumentationKind.InterpretedSnippet(
                              interpreterId
                            ) =>
                            val decorated = config
                                .scriptDecorator(interpreterId)
                                .map(
                                  _.decorate(snippet.lines)
                                )
                                .getOrElse(snippet.lines)
                            doc.addRawSnippet(
                              Markdown.Snippet(snippet.header, decorated.toSeq)
                            )
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
        checkFs: FileSystem
    ): Option[Errors] =
        val actualFile = fileSystem.toFileTree(outputDir / generated.short)
        val checkFile = checkFs.toFileTree(generated.full)

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
                    case None => Some(CheckErrors.MissingFile(generated.short))
                    case Some(FileTree.Dir(_)) =>
                        Some(CheckErrors.MissingFile(generated.short))
                    case Some(FileTree.File(af)) =>
                        val actualContent = fileSystem.content(af)
                        val expectedContent =
                            checkFs.content(expected)
                        if actualContent == expectedContent then None
                        else
                            Some(
                              CheckErrors.MismatchingContent(
                                generated.short,
                                actualContent,
                                expectedContent
                              )
                            )
