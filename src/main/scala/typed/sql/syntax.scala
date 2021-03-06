package typed.sql

import shapeless._
import shapeless.ops.adjoin.Adjoin
import typed.sql.internal.{OrderByInfer, WhereInfer}
import typed.sql.prefixes._

object syntax extends ColumnSyntax {

  object select extends ProductArgs {
    def applyProduct[A](query: A): SelectionPrefix[A] = SelectionPrefix(query)
  }

  object delete {

    def from[A, N, Rs <: HList, Ru <: HList](table: Table[A, N, Rs, Ru]): Delete[From[TRepr[A, N, Rs, Ru]], HNil] =
      Delete.create(From(table.repr), table.name)

  }

  def update[A, N, Rs <: HList, Ru <: HList](table: Table[A, N, Rs, Ru]): UpdationPrefix[A, N, Rs, Ru] = new UpdationPrefix(table)

  object insert {

    def into[A, N, Rs <: HList, Ru <: HList](table: Table[A, N, Rs, Ru]): InsertIntoPrefix[A, N, Rs, Ru] = new InsertIntoPrefix(table)
  }

  val `*` = All

  implicit class WhereSelectSyntax[S <: FSH, O](selection: Select[S, O, HNil]) {

    def where[C <: WhereCond, In0 <: HList](c: C)(implicit inf: WhereInfer.Aux[S, C, In0]): Select[S, O, In0] =
      new Select[S, O, In0] {
        type WhereFlag = Select.WhereDefined.type
        def astData: ast.Select[O] = selection.astData.copy(where = Some(inf.mkAst(c)))
        def in: In0 = inf.out(c)
      }
  }

  implicit class WhereDeleteSyntax[S <: FSH, O](deletion: Delete[S, O]) {

    def where[C <: WhereCond, In0 <: HList](c: C)(implicit inf: WhereInfer.Aux[S, C, In0]): Delete[S, In0] =
      new Delete[S, In0] {
        def astData: ast.Delete = deletion.astData.copy(where = Some(inf.mkAst(c)))
        def in: In0 = inf.out(c)
      }
  }

  implicit class WhereUpdateSyntax[S <: FSH, In1](upd: Update[S, In1]) {

    def where[C <: WhereCond, In0 <: HList, O <: HList](c: C)(
      implicit
      inf: WhereInfer.Aux[S, C, In0],
      adjoin: Adjoin.Aux[In1 :: In0 :: HNil, O]
    ): Update[S, O] =
      new Update[S, O] {
        def astData: ast.Update = upd.astData.copy(where = Some(inf.mkAst(c)))
        def in: O = adjoin(upd.in :: inf.out(c) :: HNil)
      }
  }

  implicit class OrderBySelectSyntax[S <: FSH, O, In](sel: Select[S, O, In]) {

    object orderBy extends ProductArgs {
      def applyProduct[C <: HList](c: C)(implicit inf: OrderByInfer[S, C]): Select[S, O, In] = {
        new Select[S, O, In] {
          type WhereFlag = sel.WhereFlag
          val astData: ast.Select[O] = {
            val ord = ast.OrderBy(inf.columns)
            sel.astData.copy(orderBy = Some(ord))
          }
          val in: In = sel.in
        }
      }
    }
  }

  implicit class LimitOffsetSyntax[S <: FSH, O, In](sel: Select[S, O, In]) {

    def limit[J <: HList](i: Int)(implicit adjoin: Adjoin.Aux[In :: Int :: HNil, J]): Select[S, O, J] =
        new Select[S, O, J] {
          type WhereFlag = sel.WhereFlag
          val astData: ast.Select[O] = {
            sel.astData.copy(limit = Some(i))
          }
          val in: J = adjoin(sel.in :: i :: HNil)
        }

    def offset[J <: HList](i: Int)(implicit adjoin: Adjoin.Aux[In :: Int :: HNil, J]): Select[S, O, J] =
      new Select[S, O, J] {
        type WhereFlag = sel.WhereFlag
        val astData: ast.Select[O] = {
          sel.astData.copy(offset = Some(i))
        }
        val in: J = adjoin(sel.in :: i :: HNil)
      }
  }

  implicit class JoinSyntax[A <: FSH](shape: A) {

    def innerJoin[S2, N2, Rs2 <: HList, ru <: HList](t: Table[S2, N2, Rs2, ru]): IJPrefix[A, S2, N2, Rs2, ru] =
      new IJPrefix(shape, From(t.repr))

    def leftJoin[S2, N2, Rs2 <: HList, ru <: HList](t: Table[S2, N2, Rs2, ru]): LJPrefix[A, S2, N2, Rs2, ru] =
      new LJPrefix(shape, From(t.repr))

    def rightJoin[S2, N2, Rs2 <: HList, ru <: HList](t: Table[S2, N2, Rs2, ru]): RJPrefix[A, S2, N2, Rs2, ru] =
      new RJPrefix(shape, From(t.repr))

    def fullJoin[S2, N2, Rs2 <: HList, ru <: HList](t: Table[S2, N2, Rs2, ru]): FJPrefix[A, S2, N2, Rs2, ru] =
      new FJPrefix(shape, From(t.repr))
  }

}
