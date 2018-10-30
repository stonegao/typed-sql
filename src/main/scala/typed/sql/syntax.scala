package typed.sql

import shapeless.{HList, LabelledGeneric, ProductArgs}
import typed.sql.internal.WhereInfer
import typed.sql.prefixes._

object syntax extends ColumnSyntax {

  object select extends ProductArgs {
    def applyProduct[A](query: A): SelectionPrefix[A] = SelectionPrefix(query)
  }

  val `*` = All

//  implicit class WhereSyntax[S, R, Repr1 <: HList, In1 <: HList, WF1 <: Selection.HasWhere](
//    selection: Selection[S, R] {type Repr = Repr1; type In = In1; type WhereFlag = WF1}
//  )(implicit ev: WF1 =:= Selection.WithoutWhere.type ) {
//
//    def where[C <: WhereClause, Out <: HList](clause: C)(
//      implicit whereInfer: WhereInfer.Aux[selection.Repr, C, Out]
//    ): Selection[S, R] { type Repr = Repr1; type In = Out; type WhereFlag = Selection.WhereDefined.type } = {
//      new Selection[S, R] {
//
//        type Repr = Repr1
//
//        type In = Out
//
//        type WhereFlag = Selection.WhereDefined.type
//
//        val labelledGeneric: LabelledGeneric.Aux[S, Repr] = selection.labelledGeneric
//
//        val tableName = selection.tableName
//        val fields = selection.fields
//
//        val sql: String = {
//          val fs = fields.map(f => s"$tableName.$f").mkString(", ")
//          val rendered = whereInfer.expr.render(tableName)
//          s"SELECT $fs FROM $tableName WHERE $rendered"
//        }
//
//        val in: In = whereInfer.in(clause)
//      }
//    }
//  }

  implicit class JoinSyntax[A <: FSH](shape: A) {

    def innerJoin[S2, N2, R2 <: HList](t: Table3[S2, N2, R2]): IJPrefix[A, S2, N2, R2] =
      new IJPrefix(shape, t.shape)

    def leftJoin[S2, N2, R2 <: HList](t: Table3[S2, N2, R2]): LJPrefix[A, S2, N2, R2] =
      new LJPrefix(shape, t.shape)

    def rightJoin[S2, N2, R2 <: HList](t: Table3[S2, N2, R2]): RJPrefix[A, S2, N2, R2] =
      new RJPrefix(shape, t.shape)

    def fullJoin[S2, N2, R2 <: HList](t: Table3[S2, N2, R2]): FJPrefix[A, S2, N2, R2] =
      new FJPrefix(shape, t.shape)
  }

}