package lalalang.lib.interpreters

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all.*
import lalalang.lib.Show.instances.given
import lalalang.lib.expr.BuiltinFn.*
import lalalang.lib.expr.Expr
import lalalang.lib.expr.Expr.*
import lalalang.lib.expr.model.VarName
import lalalang.lib.interpreters.EnvInterpreter.Value.{asClosure, asInt}
import lalalang.lib.interpreters.EnvInterpreter.*
import tofu.syntax.raise.*

class EnvInterpreter[F[_]: Sync: Err.Raise](debug: Boolean):
  def eval(envRef: Ref[F, Env[F]])(expr: Expr): F[Value[F]] =
    val ev = eval(envRef)
    expr match
      case Var(name) =>
        for
          _   <- dbgEnv(s"lookup var $name")(envRef)
          env <- envRef.get
          value <- env
            .get(name)
            .orRaise(Err.Unbound(name))
            .verified(_.notBlackHole)(Err.Unevaluated(name))
        yield value

      case Abs(name, body) => Value.Closure(envRef, name, body).pure

      case Lit(n) => Value.Number(n).pure

      case app @ App(body, arg) =>
        for
          _ <- dbg(s"\napp ${app.show}")

          closure  <- ev(body).asClosure
          argValue <- ev(arg) // eager arg evaluation

          _ <- dbgEnv(s"evaluated body ${closure.body.show}")(closure.env)
          _ <- dbg(s"evaluated arg ${argValue}")

          newEnv <- cloneMap(closure.env, _ + (closure.varName -> argValue))
          res    <- eval(newEnv)(closure.body)
        yield res

      case Bind(Binding(rec, name, body), inExpr) =>
        for {
          _ <- dbgEnv(s"binding, ${body.show}")(envRef)

          bedrock <-
            if (rec) cloneMap(envRef, _ + (name -> Value.BlackHole()))
            else envRef.pure

          _ <- dbgEnv(s"bedrock env")(bedrock)

          bodyVal <- eval(bedrock)(body).flatMap {
            case closure: Value.Closure[F] =>
              dbgEnv(s"evaluated to closure with env")(closure.env) >>
                envRef.get
                  .flatTap(e => closure.env.set(e + (name -> closure)))
                  .as(closure) <*
                dbgEnv(s"patched closure env")(closure.env)

            case other => other.pure
          }

          clonedEnv <- cloneMap(envRef, _ + (name -> bodyVal))
          res       <- eval(clonedEnv)(inExpr)
        } yield res

      case arith @ Builtin(Arithmetic(op, a, b)) =>
        dbg(s"arith: ${arith.show}") >>
          (ev(a).asInt, ev(b).asInt)
            .mapN(op(_, _))
            .map(Value.Number(_))
            .flatTap(x => dbg(s"eval $x"))

      case comp @ Builtin(Comparison(op, a, b)) =>
        dbg(s"comp: ${comp.show}") >>
          (ev(a).asInt, ev(b).asInt)
            .mapN(op(_, _))
            .map(Value.Number(_))

      case Cond(pred, trueBranch, falseBranch) =>
        (ev(pred).asInt).flatMap {
          case 1 => ev(trueBranch)
          case 0 => ev(falseBranch)
        }
  end eval

  extension (env: Env[F])
    def getBound(name: VarName): F[Value[F]] =
      env.get(name).orRaise(Err.Unbound(name))

  private def dbg(msg: String): F[Unit] =
    Sync[F].delay(println(msg)).whenA(debug)

  private def dbgEnv(msg: String)(ref: Ref[F, Env[F]]): F[Unit] =
    showEnvRef(ref).flatMap(e => dbg(s"$msg [$e]"))

  private def envToString(env: Env[F], iter: Int = 0): F[Map[VarName, String]] =
    env.toList
      .traverse { (key, value) =>
        val strValue = value match
          case Value.Closure(envRef, varName, body) =>
            if (iter > 2) "<inf rec>".pure
            else
              envRef.get
                .flatMap(envToString(_, iter + 1))
                .map(_.toString)
                .map(inner => s"Closure(<fn>, [$inner])")
          case other =>
            other.toString.pure
        strValue.map(key -> _)
      }
      .map(_.toMap)

  private def showEnvRef(ref: Ref[F, Env[F]]): F[Map[VarName, String]] =
    ref.get.flatMap(envToString(_, 0))

  private def cloneMap[A, B](ref: Ref[F, A], f: A => B): F[Ref[F, B]] =
    ref.get.map(f).flatMap(Ref.of)

end EnvInterpreter

object EnvInterpreter:
  type Env[F[_]] = Map[VarName, Value[F]]

  enum Value[F[_]]:
    case Number(num: Int)
    case Closure(env: Ref[F, Env[F]], varName: VarName, body: Expr)
    case BlackHole[F[_]]() extends Value[F]

  object Value:
    def blackhole[F[_]]: Value[F] = Value.BlackHole()

    extension [F[_]: Monad: Err.Raise](v: F[Value[F]])
      def asClosure: F[Value.Closure[F]] =
        v.flatMap {
          case c: Closure[F] => c.pure[F]
          case other         => Err.UnexpectedOp(other.toString, "closure").raise
        }
      def asInt: F[Int] =
        v.flatMap {
          case Number(n) => n.pure
          case other     => Err.UnexpectedOp(other.toString, "literal").raise
        }

    extension [F[_]: Monad: Err.Raise](v: Value[F])
      def checkBlackHole(name: VarName): F[Value[F]] =
        v match
          case Value.BlackHole() => Err.Unevaluated(name).raise
          case other             => other.pure

      def notBlackHole: Boolean =
        v match
          case Value.BlackHole() => false
          case _                 => true
  end Value

  enum Err(message: String) extends Exception(message):
    case UnexpectedOp(received: String, expected: String) extends Err(s"Expected ${expected}, got $received")
    case Unevaluated(name: VarName)                       extends Err(s"Unevaluated $name: BlackHole")
    case Unbound(name: VarName)                           extends Err(s"Unbound name $name")

  object Err:
    type Raise[F[_]] = tofu.Raise[F, Err]

end EnvInterpreter
