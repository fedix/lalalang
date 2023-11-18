package lalalang.lib

import lalalang.lib.model.VarName
import parsley.character.{char, digit, letter, letterOrDigit, oneOf, space, string}
import parsley.combinator.many
import parsley.{Parsley, Result}

class LCParser:
  import parseUtils.*

  def parse(input: String): Result[String, Expr] =
    term.parse(input)

  val literal: Parsley[Expr] =
    many(digit).map { numberChars =>
      Expr.Lit(numberChars.mkString.toInt)
    }

  // starts with a letter
  // may also contain digits
  val varName: Parsley[VarName] =
    letter
      .flatMap { head =>
        many(letterOrDigit).map(tail => (head :: tail).mkString)
      }
      .filterNot(LCParser.reservedKeywords.contains)

  val absName: Parsley[VarName] =
    oneOf('λ', '\\') *> varName <* char('.')

  val abs: Parsley[Expr.Abs] =
    for
      name <- absName
      body <- term
    yield Expr.Abs(name, body)

  // if (...) {...} else {...}
  val cond: Parsley[Expr.Cond] =
    for
      pred        <- string("if ") *> brackets(term) <* space
      trueBranch  <- squigglyBrackets(term)
      falseBranch <- string(" else ") *> squigglyBrackets(term)
    yield Expr.Cond(pred, trueBranch, falseBranch)

  val nonApp: Parsley[Expr] =
    cond <|> brackets(term) <|> abs <|> varName.map(Expr.Var(_)) <|> literal

  val term: Parsley[Expr] =
    chainl1(nonApp, space #> Expr.App.apply)

object LCParser:
  private val reservedKeywords = Set("if", "else", "λ")
