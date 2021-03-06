package typed.sql.prefixes

import shapeless.{HList, HNil}
import typed.sql._
import typed.sql.internal.SelectInfer

case class SelectionPrefix[Q](query: Q) {

  def from[S <: FSH, O](shape: S)(
    implicit
    inf: SelectInfer.Aux[S, Q, O]
  ): Select[S, O, HNil] = Select.create(inf.mkAst(shape))

  def from[A, N, Rs <: HList, Ru <: HList, O](table: Table[A, N, Rs, Ru])(
    implicit
    inf: SelectInfer.Aux[From[TRepr[A, N, Rs, Ru]], Q, O]
  ): Select[From[TRepr[A, N, Rs, Ru]], O, HNil] = Select.create(inf.mkAst(From(table.repr)))

}
