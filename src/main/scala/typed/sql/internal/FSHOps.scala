package typed.sql.internal

import shapeless._
import typed.sql._

import scala.annotation.implicitNotFound

object FSHOps {

  trait IsFieldOf[A <: FSH, Name]

  object IsFieldOf {

    implicit def forFrom[N, N2, a, r <: HList](
      implicit
      ev: N =:= N2
    ): IsFieldOf[From[TRepr[a, N, r]], N2] = null

    implicit def forIJ[N, N2, a, r <: HList, c <: JoinCond, t <: FSH](
      implicit ev: N =:= N2
    ): IsFieldOf[IJ[TRepr[a, N, r], c, t], N2] = null

    implicit def forIJRecurse[T <: FSH, a, n2, r <: HList, N, R, c <: JoinCond](
      implicit next: IsFieldOf[T, N]
    ): IsFieldOf[IJ[TRepr[a, n2, r], c, T], N] = null

    implicit def forLJ[N, N2, a, r <: HList, c <: JoinCond, t <: FSH](
      implicit ev: N =:= N2
    ): IsFieldOf[LJ[TRepr[a, N, r], c, t], N2] = null

    implicit def forLJRecurse[T <: FSH, a, n2, r <: HList, N, R, c <: JoinCond](
      implicit next: IsFieldOf[T, N]
    ): IsFieldOf[LJ[TRepr[a, n2, r], c, T], N] = null

    implicit def forRJ[N, N2, a, r <: HList, c <: JoinCond, t <: FSH](
      implicit ev: N =:= N2
    ): IsFieldOf[RJ[TRepr[a, N, r], c, t], N2] = null

    implicit def forRJRecurse[T <: FSH, a, n2, r <: HList, N, R, c <: JoinCond](
      implicit next: IsFieldOf[T, N]
    ): IsFieldOf[RJ[TRepr[a, n2, r], c, T], N] = null

    implicit def forFJ[N, N2, a, r <: HList, c <: JoinCond, t <: FSH](
      implicit ev: N =:= N2
    ): IsFieldOf[FJ[TRepr[a, N, r], c, t], N2] = null

    implicit def forFJRecurse[T <: FSH, a, n2, r <: HList, N, R, c <: JoinCond](
      implicit next: IsFieldOf[T, N]
    ): IsFieldOf[FJ[TRepr[a, n2, r], c, T], N] = null

    //def apply[A <: FSH, N](implicit v: IsFieldOf[A, N]): IsFieldOf[A, N] = v
    def apply[A <: FSH](fsh: A, k: Witness)(implicit v: IsFieldOf[A, k.T]): IsFieldOf[A, k.T] = v
  }

  @implicitNotFound("\nCan't prove that join operation is possible \n${A} \n\twith \n\t${B} \n\tusing\n\t${C}. \nCheck that you use columns that belongs to joining tables")
  trait CanJoin[A <: FSH, B <: From[_], C <: JoinCond]
  object CanJoin {

    implicit def canJoinEq1[A <: FSH, R <: TR, B <: From[_], T1, T2, k1, k2, v](
      implicit
      f1: IsFieldOf[A, T1],
      f2: IsFieldOf[B, T2]
    ): CanJoin[A, B, JoinCond.Eq[k1, v, T1, k2, T2]] = null

    implicit def canJoinEq2[A <: FSH, R <: TR, B <: From[_], T1, T2, k1, k2, v](
      implicit
      f1: IsFieldOf[A, T2],
      f2: IsFieldOf[B, T1]
    ): CanJoin[A, B, JoinCond.Eq[k1, v, T1, k2, T2]] = null
  }


  trait AllColumns[A <: FSH] {
    def columns: List[ast.Col]
  }

  object AllColumns {

    trait FromRepr[T <: TR] {
      def columns: List[ast.Col]
    }
    object FromRepr {

      implicit def fromRepr[a, N, R <: HList](
        implicit
        fieldNames: FieldNames[R],
        wt: Witness.Aux[N],
        ev: N <:< Symbol
      ): FromRepr[TRepr[a, N, R]] = {
        new FromRepr[TRepr[a, N, R]] {
          override def columns: List[ast.Col] = {
            val table = wt.value.name
            fieldNames().map(n => ast.Col(table, n))
          }
        }

      }
    }

    private def create[A <: FSH](f: => List[ast.Col]): AllColumns[A] = new AllColumns[A] {
      override def columns: List[ast.Col] = f
    }

    implicit def forFrom[T <: TR](
      implicit fromRepr: FromRepr[T]
    ): AllColumns[From[T]] = create(fromRepr.columns)

    implicit def forIJ[T <: TR, c <: JoinCond, tail <: FSH](
      implicit
      fromRepr1: FromRepr[T],
      rest: AllColumns[tail]
    ): AllColumns[IJ[T, c, tail]] = create(fromRepr1.columns ++ rest.columns)

    implicit def forLJ[T <: TR, c <: JoinCond, tail <: FSH](
      implicit
      fromRepr1: FromRepr[T],
      rest: AllColumns[tail]
    ): AllColumns[LJ[T, c, tail]] = create(fromRepr1.columns ++ rest.columns)

    implicit def forRJ[T <: TR, c <: JoinCond, tail <: FSH](
      implicit
      fromRepr1: FromRepr[T],
      rest: AllColumns[tail]
    ): AllColumns[RJ[T, c, tail]] = create(fromRepr1.columns ++ rest.columns)

    implicit def forFJ[T <: TR, c <: JoinCond, tail <: FSH](
      implicit
      fromRepr1: FromRepr[T],
      rest: AllColumns[tail]
    ): AllColumns[FJ[T, c, tail]] = create(fromRepr1.columns ++ rest.columns)
  }


  trait JoinCondInfer[C <: JoinCond] {
    def mkAst(): ast.JoinCond
  }

  // TODO AND ? OR?
  object JoinCondInfer {

    implicit def forEq[K1, v, T1, K2, T2](
      implicit
      wt1: Witness.Aux[K1],
      ev1: K1 <:< Symbol,
      wt2: Witness.Aux[T1],
      ev2: T1 <:< Symbol,
      wt3: Witness.Aux[K2],
      ev3: K2 <:< Symbol,
      wt4: Witness.Aux[T2],
      ev4: T2 <:< Symbol,
    ): JoinCondInfer[JoinCond.Eq[K1, v, T1, K2, T2]] = {
      new JoinCondInfer[JoinCond.Eq[K1, v, T1, K2, T2]] {
        override def mkAst(): ast.JoinCond =
          ast.JoinCondEq(ast.Col(wt2.value.name, wt1.value.name), ast.Col(wt4.value.name, wt3.value.name))
      }
    }
  }

  trait FromInfer[A <: FSH, Q] {
    type Out
    def mkAst(shape: A): ast.From
  }


  trait LowPrioFromInfer {

    type Aux[A <: FSH, Q, Out0] = FromInfer[A, Q] { type Out = Out0 }

  }

  //TODO: flatten tuples?
  object FromInfer extends LowPrioFromInfer {

    implicit def starFrom[A, N, r <: HList](
      implicit
      wt: Witness.Aux[N],
      ev: N <:< Symbol
    ):Aux[From[TRepr[A, N, r]], All.type :: HNil, A] = {
      new FromInfer[From[TRepr[A, N, r]], All.type :: HNil] {
        type Out = A
        def mkAst(shape: From[TRepr[A, N, r]]): ast.From = ast.From(wt.value.name, List.empty)
      }
    }

    implicit def starIJ[A, N, r <: HList, A2, N2, r2 <: HList, C <: JoinCond, tail <: FSH, O1](
      implicit
      wt: Witness.Aux[N],
      ev: N <:< Symbol,
      cndInf: JoinCondInfer[C],
      tInf: FromInfer.Aux[tail, All.type :: HNil, O1]
    ):Aux[IJ[TRepr[A, N, r], C, tail], All.type :: HNil, (O1, A)] = {
      new FromInfer[IJ[TRepr[A, N, r], C, tail], All.type :: HNil] {
        type Out = (O1, A)
        def mkAst(shape: IJ[TRepr[A, N, r], C, tail]): ast.From = {
          val x = tInf.mkAst(shape.tail)
          val j = ast.InnerJoin(wt.value.name, cndInf.mkAst())
          ast.From(x.table, x.joins :+ j)
        }
      }
    }

    implicit def starLJ[A, N, r <: HList, A2, N2, r2 <: HList, C <: JoinCond, tail <: FSH, O1](
      implicit
      wt: Witness.Aux[N],
      ev: N <:< Symbol,
      cndInf: JoinCondInfer[C],
      tInf: FromInfer.Aux[tail, All.type :: HNil, O1]
    ):Aux[LJ[TRepr[A, N, r], C, tail], All.type :: HNil, (O1, Option[A])] = {
      new FromInfer[LJ[TRepr[A, N, r], C, tail], All.type :: HNil] {
        type Out = (O1, Option[A])
        def mkAst(shape: LJ[TRepr[A, N, r], C, tail]): ast.From = {
          val x = tInf.mkAst(shape.tail)
          val j = ast.LeftJoin(wt.value.name, cndInf.mkAst())
          ast.From(x.table, x.joins :+ j)
        }
      }
    }

    implicit def starRJ[A, N, r <: HList, A2, N2, r2 <: HList, C <: JoinCond, tail <: FSH, O1](
      implicit
      wt: Witness.Aux[N],
      ev: N <:< Symbol,
      cndInf: JoinCondInfer[C],
      tInf: FromInfer.Aux[tail, All.type :: HNil, O1]
    ):Aux[RJ[TRepr[A, N, r], C, tail], All.type :: HNil, (Option[O1], A)] = {
      new FromInfer[RJ[TRepr[A, N, r], C, tail], All.type :: HNil] {
        type Out = (Option[O1], A)
        def mkAst(shape: RJ[TRepr[A, N, r], C, tail]): ast.From = {
          val x = tInf.mkAst(shape.tail)
          val j = ast.RightJoin(wt.value.name, cndInf.mkAst())
          ast.From(x.table, x.joins :+ j)
        }
      }
    }

    implicit def starFJ[A, N, r <: HList, A2, N2, r2 <: HList, C <: JoinCond, tail <: FSH, O1](
      implicit
      wt: Witness.Aux[N],
      ev: N <:< Symbol,
      cndInf: JoinCondInfer[C],
      tInf: FromInfer.Aux[tail, All.type :: HNil, O1]
    ):Aux[FJ[TRepr[A, N, r], C, tail], All.type :: HNil, (Option[O1], Option[A])] = {
      new FromInfer[FJ[TRepr[A, N, r], C, tail], All.type :: HNil] {
        type Out = (Option[O1], Option[A])
        def mkAst(shape: FJ[TRepr[A, N, r], C, tail]): ast.From = {
          val x = tInf.mkAst(shape.tail)
          val j = ast.RightJoin(wt.value.name, cndInf.mkAst())
          ast.From(x.table, x.joins :+ j)
        }
      }
    }
  }


}
