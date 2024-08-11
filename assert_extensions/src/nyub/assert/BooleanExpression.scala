package nyub.assert

import scala.quoted.*

case class BooleanExpression(val booleanValue: Boolean, val repr: String)

object BooleanExpression:
    @annotation.nowarn("msg=Unstable inline accessor")
    inline def booleanExpression(inline b: Boolean): BooleanExpression = ${
        booleanExpressionImpl('b)
    }

    private def booleanExpressionImpl(using Quotes)(
        b: Expr[Boolean]
    ): Expr[BooleanExpression] =
        import quotes.reflect.*
        val repr = b.asTerm.show(using Printer.TreeShortCode)
        '{ BooleanExpression($b, ${ Expr(repr) }) }
