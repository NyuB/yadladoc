package nyub.assert

import nyub.assert.CaseClauses.caseClauses

@annotation.nowarn("msg=unused local definition")
class CaseClausesSuite extends munit.FunSuite with AssertExtensions:
    test("Inlined match clauses representation"):
        val clauses = caseClauses:
            case i: Int    => true
            case d: Double => true
            case s: String => false
        clauses.repr `is equal to` """
            case i: Int =>
              true
            case d: Double =>
              true
            case s: String =>
              false
            """.stripIndent().stripPrefix("\n").stripSuffix("\n")

        clauses.isDefinedAt(0) `is equal to` true
        clauses(0) `is equal to` true
        clauses(0.0) `is equal to` true
        clauses.isDefinedAt("S") `is equal to` true
        clauses("S") `is equal to` false

    test("Raise compile error if not using properly inlined case clauses"):
        val pf: PartialFunction[Int, Unit] = { case 0 => () }
        compileErrors(
          "caseClauses(pf)"
        ) `contains substring` "expected an inlined list of case clauses"

end CaseClausesSuite
