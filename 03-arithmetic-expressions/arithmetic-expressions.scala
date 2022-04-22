import scala.language.implicitConversions

object AEId {

sealed abstract class Exp

case class Num(n: Int) extends Exp
case class Add(lhs: Exp, rhs: Exp) extends Exp
case class Mul(lhs: Exp, rhs: Exp) extends Exp
case class Id(x: String) extends Exp

 val test0 = Add(Mul(Id("x"),Num(2)),Add(Id("y"),Id("y")))

implicit def num2exp(n: Int) = Num(n)
implicit def sym2exp(x: String) = Id(x)

 val test = Add(Mul("x",2),Add("y","y"))

type Env = Map[String,Int]

 def eval(e: Exp, env: Env) : Int = e match {
  case Num(n) => n
  case Id(x) => env(x)
  case Add(l,r) => eval(l,env) + eval(r,env)
  case Mul(l,r) => eval(l,env) * eval(r,env)
}

val testEnv = Map("x" -> 3, "y" -> 4)

assert(eval(test, testEnv) == 14)

}

object Visitors {
  case class Visitor[T](num: Int => T, add: (T, T) => T)
  // an alternative to this design is to define num and add as abstract methods
  // and then create concrete visitors by subclassing or trait composition.

  sealed abstract class Exp

  case class Num(n: Int) extends Exp
  case class Add(lhs: Exp, rhs: Exp) extends Exp

  def foldExp[T](v: Visitor[T], e: Exp): T = {
    e match {
      case Num(n) => v.num(n)
      case Add(l, r) => v.add(foldExp(v, l), foldExp(v, r))
    }
  }

  val evalVisitor = Visitor[Int](x => x, (a, b) => a + b)

  def eval(e: Exp) = foldExp(evalVisitor, e)

  assert(eval(Add(Add(Num(1),Num(2)),Num(3))) == 6)

  val countVisitor = Visitor[Int]( _=>1, _+_)
  val printVisitor = Visitor[String](_.toString, "("+_+"+"+_+")")

}

object AEIdVisitor {
  import AEId._

  case class Visitor[T](num: Int => T, add: (T, T) => T, mul: (T, T) => T, id: String => T)
  val expVisitor = Visitor[Exp](Num(_), Add(_, _), Mul(_, _), Id(_))
  val countVisitor = Visitor[Int](_=>1, _ + _, _ + _, _ => 0)
  val printVisitor = Visitor[String](_.toString, "(" + _ + "+" + _ + ")", _ + "*" + _, _.x)

  def foldExp[T](v: Visitor[T], e: Exp) : T = {
    e match {
      case Num(n) => v.num(n)
      case Add(l,r) => v.add(foldExp(v, l), foldExp(v, r))
      case Mul(l,r) => v.mul(foldExp(v, l), foldExp(v, r))
      case Id(x) => v.id(x)
    }
  }

  def countNums(e: Exp) = foldExp(countVisitor, e)

  assert(countNums(test) == 1)

  val evalVisitor = Visitor[Env=>Int](
     env => _ ,
     (a, b) => env =>
       a(env) + b(env),
     (a, b) => env =>
       a(env) * b(env),
     x => env =>
       env(x))
}
