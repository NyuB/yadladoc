package nyub.assert

import scala.quoted.*

class CaseClauses[A, B](private val pf: PartialFunction[A, B], val repr: String)
    extends PartialFunction[A, B]:
    override def apply(x: A): B = pf(x)
    override def isDefinedAt(x: A): Boolean = pf.isDefinedAt(x)

object CaseClauses:
    @annotation.nowarn("msg=Unstable inline accessor")
    inline def caseClauses[A, B](inline pf: PartialFunction[A, B]) = ${
        caseClausesImpl('pf)
    }

    private def caseClausesImpl[A, B](using Quotes, Type[A], Type[B])(
        pf: Expr[PartialFunction[A, B]]
    ): Expr[CaseClauses[A, B]] =
        import quotes.reflect.*
        val tree: Tree = pf.asTerm
        unrollInlined(tree) match
            case Block(List(DefDef(_, _, _, Some(Match(_, caseDefs)))), _) =>
                val repr = caseDefs
                    .map(d => d.show(using Printer.TreeShortCode))
                    .mkString("\n")
                '{ CaseClauses(${ pf }, ${ Expr(repr) }) }
            case _ =>
                report.errorAndAbort(
                  s"Unexpected usage of CaseClauses, expected an inlined list of case clauses, got ${tree
                          .show(using Printer.TreeStructure)}",
                  pf
                )

    private def unrollInlined(using Quotes)(
        t: quotes.reflect.Tree
    ): quotes.reflect.Tree =
        import quotes.reflect.*
        t match
            case Inlined(
                  _,
                  _,
                  i: Inlined
                ) =>
                unrollInlined(i)
            case Inlined(
                  _,
                  _,
                  any
                ) =>
                any
            case _ =>
                throw IllegalArgumentException(
                  s"Expected inlined expression, got ${t.show(using Printer.TreeStructure)}"
                )
