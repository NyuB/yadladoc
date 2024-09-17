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

    private val exampleAssembler = ExampleAssembler(
      templating,
      id => config.templateFile(id).useLines(_.toList),
      config.constants.snippetInjectionKey,
      config.constants.exampleNameInjectionKey,
      config.constants.subExampleNameInjectionKey,
      config.properties.toMap
    )

    def run(outputDir: Path, markdownFile: Path): Results[Seq[GeneratedFile]] =
        run(outputDir, markdownFile, fileSystem)

    def check(
        outputDir: Path,
        markdownFile: Path
    ): Results[Seq[GeneratedFile]] =
        val checkFs = InMemoryFileSystem.init()
        val checkDir = checkFs.createTempDirectory("check")

        run(checkDir, markdownFile, checkFs)
            .flatMap: results =>
                val errors = results
                    .map(checkGeneratedFile(_, outputDir, checkFs))
                    .flatMap(_.toList)
                    .toList
                Results(results, errors)

    private def run(
        outputDir: Path,
        markdownFile: Path,
        writeFs: FileSystem
    ): Results[Seq[GeneratedFile]] =
        val markdownLines = markdownFile.useLines(_.toSeq)
        val docGen = dogGenFromMarkdown(Markdown.parse(markdownLines))

        val generatedExamples = docGen
            .map(_.exampleSnippets.examples.values)
            .flatMap: examples =>
                val generated =
                    for example <- examples yield
                        val fullExample = exampleAssembler.assemble(example)
                        val exampleFile =
                            config.exampleFile(example.name, example.language)

                        writeFs.writeContent(
                          outputDir / exampleFile,
                          fullExample.mkString("\n")
                        )

                        GeneratedFile(
                          Some(outputDir),
                          exampleFile,
                          markdownFile
                        )
                Results.success(generated)

        val generatedMarkdown = docGen
            .map(_.markdownDecoration.decoratedLines)
            .flatMap: decoratedLines =>
                if decoratedLines == markdownLines then Results.success(None)
                else
                    writeFs.writeContent(
                      markdownFile,
                      decoratedLines.mkString("\n")
                    )
                    Results.success(
                      Some(GeneratedFile(None, markdownFile, markdownFile))
                    )

        generatedExamples.merge(generatedMarkdown): (examples, markdown) =>
            Results.success(examples.toSeq ++ markdown.toList)

    private def dogGenFromMarkdown(
        markdown: Iterable[Markdown.Block]
    ): Results[DocumentationGeneration] =
        val init = Results.success(DocumentationGeneration.init(config))
        val docGen = markdown.foldLeft(init): (docResults, block) =>
            block match
                case snippet: Markdown.Snippet =>
                    val docSnippet = snippet.toDocSnippet
                    config.documentationKindForSnippet(
                      docSnippet
                    ) match
                        case example: DocumentationKind.ExampleSnippet =>
                            docResults.map(
                              _.addExampleSnippet(snippet, example)
                            )
                        case DocumentationKind.DecoratedSnippet(
                              decoratorId
                            ) =>
                            docResults.tryToAddDecoratedSnippet(
                              decoratorId,
                              docSnippet.properties,
                              snippet
                            )
                        case DocumentationKind.Raw =>
                            docResults.map(
                              _.addRawBlock(snippet)
                            ) // no doc to generate
                case raw: Markdown.Raw =>
                    docResults.map(_.addRawBlock(raw)) // no doc to generate
        docGen

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

    extension (docResults: Results[DocumentationGeneration])
        private def tryToAddDecoratedSnippet(
            decoratorId: String,
            properties: Properties,
            snippet: Markdown.Snippet
        ): Results[DocumentationGeneration] =
            val decorated = config
                .scriptDecorator(
                  decoratorId,
                  properties
                )
                .map(
                  _.decorate(snippet.lines)
                )
            decorated
                .map(d =>
                    docResults.map(
                      _.addRawBlock(
                        Markdown.Snippet(
                          snippet.header,
                          d.toSeq
                        )
                      )
                    )
                )
                .getOrElse(
                  docResults
                      .withError(
                        MissingDecoratorError(decoratorId)
                      )
                      .map(_.addRawBlock(snippet))
                )
