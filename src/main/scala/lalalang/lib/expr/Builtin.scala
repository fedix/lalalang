package lalalang
package lib
package expr

import scala.util.chaining.*

// TODO: abstract to 2-ary fn?
enum ArithmeticFn:
  case Add, Sub, Mul, Div

  def mapping: ArithmeticFn => (Int, Int) => Int =
    case Add => (_ + _)
    case Sub => (_ - _)
    case Mul => (_ * _)
    case Div => (_ / _)

  def apply(a: Int, b: Int) = mapping(this)(a, b)

  def applyExpr(a: Expr, b: Expr): Expr.Lit =
    apply(Expr.asInt(a), Expr.asInt(b)).pipe(Expr.Lit(_))

enum ComparisonFn:
  case Lt, Eq, Gt

  def mapping: ComparisonFn => (Int, Int) => Boolean =
    case Lt => (_ < _)
    case Eq => (_ == _)
    case Gt => (_ > _)

  def apply(a: Int, b: Int) =
    if (mapping(this)(a, b)) 1
    else 0

  def applyExpr(a: Expr, b: Expr): Expr.Lit =
    Expr.Lit(apply(Expr.asInt(a), Expr.asInt(b)))

enum BuiltinFn:
  case Arithmetic(fn: ArithmeticFn, a: Expr, b: Expr)
  case Comparison(fn: ComparisonFn, a: Expr, b: Expr)
