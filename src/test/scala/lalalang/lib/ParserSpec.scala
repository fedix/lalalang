package lalalang
package lib

import Show.instances.given

class ParserSpec extends munit.FunSuite:
  import lalalang.functions.*

  val parser = LCParser()

  def testParser(input: String, expected: Expr): Unit =
    parser
      .parse(input)
      .fold(
        fail(_),
        assertEquals(_, expected)
      )

  test("Variable should not start with a digit") {
    val parseResult = parser.parse("λ1f.1f")

    parseResult
      .fold(
        _ => assert(true),
        _ => fail("shouldn't have parsed")
      )
  }

  test("Should parse exprsession correctly") {
    testParser("λf.(λx.f (x x)) λx.f (x x)", Y)
  }

  test("Should treat λ and \\ equally") {
    val withLambdas = "λf.(λx.f (x x)) λx.f (x x)"
    val withSlashes = withLambdas.replace('λ', '\\')

    testParser(withLambdas, Y)
    testParser(withSlashes, Y)
  }

  test("Should parse generated expression") {
    testParser(Y.show, Y)
  }

  test("Should parse variable") {
    testParser("M", Expr.Var("M"))
  }

  test("Should parse conditional") {
    testParser(
      "if ((λx.(x)) a) {T} else {F}",
      Expr.Cond(
        Expr.App(
          Expr.Abs("x", Expr.Var("x")),
          Expr.Var("a")
        ),
        Expr.Var("T"),
        Expr.Var("F")
      )
    )
  }

  test("Should parse nested conditional") {
    testParser(
      "if (if ((λx.(x)) a) {T} else {F}) {T} else {F}",
      Expr.Cond(
        Expr.Cond(
          Expr.App(
            Expr.Abs("x", Expr.Var("x")),
            Expr.Var("a")
          ),
          Expr.Var("T"),
          Expr.Var("F")
        ),
        Expr.Var("T"),
        Expr.Var("F")
      )
    )
  }
