package lalalang.lib

import parsley.{Parsley, Result}
import parsley.character.{letterOrDigit, char, space}
import parsley.combinator.manyN

class LCParser:
  import parseUtils.*

  def parse(input: String): Result[String, Expr] =
    parseTerm.parse(input)

  val varName: Parsley[String] =
    manyN(1, letterOrDigit).map(_.mkString)

  val absName: Parsley[String] =
    char('λ') *> varName <* char('.')

  val abs: Parsley[Expr.Abs] =
    for
      name <- absName
      body <- parseTerm
    yield Expr.Abs(name, body)

  val nonApp: Parsley[Expr] =
    parens(parseTerm) <|> abs <|> varName.map(Expr.Var(_))

  val parseTerm: Parsley[Expr] =
    chainl1(nonApp, space #> Expr.App.apply)
