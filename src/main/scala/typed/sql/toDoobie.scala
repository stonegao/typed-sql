package typed.sql

import doobie.util.Read
import doobie.util.fragment.Fragment
import doobie.util.param.Param
import doobie.util.query.Query0
import shapeless.HNil

object toDoobie {

  private def renderSel(s: ast.Select[_]): String = {
    def renderWCOnd(wc: ast.WhereCond): String = wc match {
      case ast.WhereEq(col) => s"${col.table}.${col.name} = ?"
      case ast.Less(col) => s"${col.table}.${col.name} < ?"
      case ast.LessOrEq(col) => s"${col.table}.${col.name} <= ?"
      case ast.Gt(col) => s"${col.table}.${col.name} > ?"
      case ast.GtOrEq(col) => s"${col.table}.${col.name} >= ?"
      case ast.Like(col) => s"${col.table}.${col.name} like ?"
      case ast.And(c1, c2) => renderWCOnd(c1) + " AND " + renderWCOnd(c2)
      case ast.Or(c1, c2) => renderWCOnd(c1) + " OR " + renderWCOnd(c2)
    }

    def joinCondRender(jc: ast.JoinCond): String = jc match {
      case ast.JoinCondEq(col1, col2) => s"${col1.table}.${col1.name} = ${col2.table}.${col2.name}"
      case ast.JoinCondAnd(c1, c2) => joinCondRender(c1) + " AND " + joinCondRender(c2)
      case ast.JoinCondOr(c1, c2) => joinCondRender(c1) + " OR " + joinCondRender(c2)
    }

    def joinRender(j: ast.Join): String = j match {
      case ast.InnerJoin(t, c) => s"INNER JOIN $t ON " + joinCondRender(c)
      case ast.LeftJoin(t, c) => s"LEFT JOIN $t ON " + joinCondRender(c)
      case ast.RightJoin(t, c) => s"RIGHT JOIN $t ON " + joinCondRender(c)
      case ast.FullJoin(t, c) =>  s"FULL JOIN $t ON " + joinCondRender(c)
    }

    val cols = s.cols.map(c => s"${c.table}.${c.name}").mkString(", ")
    val joins = s.from.joins.map(joinRender).mkString(" ")
    val from = s.from.table
    val whereR = s.where.map(c => " WHERE " + renderWCOnd(c)).getOrElse("")
    s"SELECT $cols FROM $from $joins" + whereR
  }

  implicit class WrapSelection[S <: FSH, Out, In](sel: Selection[S, Out, In]) {

    def toFragment(implicit param: Param[In]): Fragment = {
      val sql = renderSel(sel.astData)
      Fragment[In](sql, sel.in, None)(param.write)
    }

    def toQuery(implicit param: Param[In], read: Read[Out]): Query0[Out] = toFragment.query[Out](read)
  }
}
