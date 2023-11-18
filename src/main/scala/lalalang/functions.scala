package lalalang
package functions

import lalalang.lib.ArithmeticFn.*
import lalalang.lib.BuiltinFn.*
import lalalang.lib.Expr.*
import lalalang.lib.*

def lit: Int => Expr = Lit(_)

def inc: Expr = Abs(
  variable = "x",
  body = Builtin(Arithmetic(Add, Var("x"), Lit(1)))
)

def incApply(n: Int): Expr = App(inc, Lit(n))

def identityApply(n: Int): Expr =
  App(
    Abs(
      variable = "x",
      body = Var("x")
    ),
    Lit(n)
  )

private def lambda2(variables: (String, String), body: Expr) = {
  val (a, b) = variables
  Abs(a, Abs(b, body))
}

// will recurse forever in case of eager arg evaluation
def lazyFixpoint = {
  val xx = App(Var("x"), Var("x"))

  val inner =
    Abs(
      variable = "x",
      body = App(Var("f"), xx)
    )

  Abs("f", App(inner, inner))
}

def eagerFixpoint = {
  val xx          = App(Var("x"), Var("x"))
  val indirection = Abs("v", App(xx, Var("v")))

  val inner =
    Abs(
      variable = "x",
      body = App(Var("f"), indirection)
    )

  Abs("f", App(inner, inner))
}

val fibStep = {
  def xMinus(n: Int) = Builtin(
    Arithmetic(ArithmeticFn.Sub, Var("x"), Lit(n))
  )

  val falseBranch = Builtin(
    Arithmetic(
      ArithmeticFn.Add,
      App(Var("f"), xMinus(1)),
      App(Var("f"), xMinus(2))
    )
  )

  Abs(
    "f",
    Abs(
      "x",
      Cond(
        Builtin(Comparison(ComparisonFn.Lt, Var("x"), Lit(2))),
        Lit(1),
        falseBranch
      )
    )
  )
}

def fib(n: Int, _lazy: Boolean) = {
  val fn = App(if (_lazy) lazyFixpoint else eagerFixpoint, fibStep)
  App(fn, Lit(n))
}

def fibDirect(n: Int) = {
  def xMinus(n: Int) = Builtin(
    Arithmetic(ArithmeticFn.Sub, Var("x"), Lit(n))
  )

  val falseBranch = Builtin(
    Arithmetic(
      ArithmeticFn.Add,
      App(Var("fib"), xMinus(1)),
      App(Var("fib"), xMinus(2))
    )
  )
  // let rec fib = \x -> if (x < 2) 1 else fib (x - 1) + fib (x - 2)
  Bind(
    recursive = true,
    name = "fib",
    bindingBody = Abs(
      variable = "x",
      body = Cond(
        pred = Builtin(Comparison(ComparisonFn.Lt, Var("x"), Lit(2))),
        trueBranch = Lit(1),
        falseBranch = falseBranch
      )
    ),
    expr = App(Var("fib"), lit(n))
  )
}

object booleans {
  def t = Abs("t", Abs("f", body = Var("t")))
  def f = Abs("t", Abs("f", body = Var("f")))

  def and =
    lambda2(
      ("p", "q"),
      App(App(Var("p"), Var("q")), Var("p"))
    )

  def andtf = App(App(and, t), f)

  def andt = App(and, t)

  def tf  = App(t, f)
  def tft = App(tf, t)
}
