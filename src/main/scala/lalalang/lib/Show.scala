package lalalang
package lib

trait Show[T]:
  extension (t: T) def show: String

object Show:
  import Expr.*, ArithmeticFn.*, ComparisonFn.*, BuiltinFn.*

  def apply[T: Show]: Show[T] = summon

  def instance[T](showFn: T => String): Show[T] = showFn(_)

  object instances:
    given Show[ArithmeticFn] = instance {
      case Add => "+"
      case Sub => "-"
      case Mul => "*"
      case Div => "/"
    }

    given Show[ComparisonFn] = instance {
      case Lt => "<"
      case Eq => "=="
      case Gt => ">"
    }

    given Show[BuiltinFn] = instance {
      case Arithmetic(op, a, b) => s"${a.show} ${op.show} ${b.show}"
      case Comparison(op, a, b) => s"${a.show} ${op.show} ${b.show}"
    }

    given Show[Expr] = instance {
      case Var(name)           => name
      case Abs(variable, body) => s"λ$variable.${body.show}"
      case App(expr, arg)      => s"(${expr.show}) (${arg.show})"
      case Lit(x)              => x.toString
      case Builtin(fn)         => fn.show
      case Cond(pred, trueBranch, falseBranch) =>
        s"if (${pred.show}) then {${trueBranch.show}} else {${falseBranch.show}}"
      case Bind(rec, name, body, expr) => s"${expr.show} [${if (rec) "rec" else ""} $name = ${body.show}]"
    }
