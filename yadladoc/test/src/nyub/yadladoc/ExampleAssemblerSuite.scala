package nyub.yadladoc

import nyub.assert.AssertExtensions
import nyub.templating.SurroundingTemplateInjection

class ExampleAssemblerSuite extends munit.FunSuite with AssertExtensions:
    test("Sanitized example name available in main template"):
        val example = Example(
          "a/b.txt",
          templateId,
          None,
          Seq(
            bodyOnlyExampleContent("Line #1")
          )
        )

        assembler.assemble(example) `is equal to`
            Seq("a_b_txt", "Line #1")

    test("Sanitized & indexed example name available in prefix template"):
        val contentWithPrefix =
            ExampleContent(Seq(prefixTemplateId), Seq("Body"), Seq.empty)
        val example = Example("a.txt", templateId, None, Seq(contentWithPrefix))

        assembler.assemble(example) `is equal to` Seq(
          "a_txt",
          "a_txt_0\nBody"
        )

    test("Sanitized & indexed example name available in suffix template"):
        val contentWithPrefix =
            ExampleContent(Seq.empty, Seq("Body"), Seq(suffixTemplateId))
        val example = Example("a.txt", templateId, None, Seq(contentWithPrefix))

        assembler.assemble(example) `is equal to` Seq(
          "a_txt",
          "Body\na_txt_0"
        )

    def assembler = ExampleAssembler(
      templateInjection,
      getTemplate,
      bodyInjectionKey,
      exampleNameInjectionKey,
      partNameInjectionKey,
      Map.empty
    )

    private val exampleNameInjectionKey = "exampleNameInjectionKey"
    private val partNameInjectionKey = "partNameInjectionKey"
    private val templateInjection = SurroundingTemplateInjection("<<", ">>")
    private val bodyInjectionKey = "bodyInjectionKey"
    private val testTemplate =
        Seq(
          s"<<$exampleNameInjectionKey>>",
          s"<<$bodyInjectionKey>>"
        )

    private val testPrefixTemplate =
        Seq(
          s"<<$partNameInjectionKey>>"
        )

    private val testSuffixTemplate = testPrefixTemplate

    private val templateId = TemplateId("templateId")
    private val prefixTemplateId = TemplateId("prefixId")
    private val suffixTemplateId = TemplateId("suffixId")
    private def getTemplate(id: TemplateId): Iterable[String] =
        if id == templateId then testTemplate
        else if id == suffixTemplateId then testSuffixTemplate
        else if id == prefixTemplateId then testPrefixTemplate
        else throw IllegalStateException(s"Unknown template id $id")

    private def bodyOnlyExampleContent(
        lines: String*
    ): ExampleContent = ExampleContent(Seq.empty, lines, Seq.empty)

end ExampleAssemblerSuite
