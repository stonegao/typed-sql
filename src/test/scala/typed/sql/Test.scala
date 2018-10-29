package typed.sql

import org.scalatest.FunSpec

case class TestRow(
  a: Int,
  b: String,
  c: Boolean
)

case class Row2(
  first: Int,
  second: String,
  third: Boolean,
  fourth: Long
)

case class Row3(
  x: Int,
  y: String
)

class Test extends FunSpec {

  it("test dsl") {

    import typed.sql.syntax._

    val table1 = Table3.of[TestRow].name('my_table)
    val table2 = Table3.of[Row2].name('second_table)
    val table3 = Table3.of[Row3].name('third)

    val a1 = table1.col('a)
    val first2 = table2.col('first)
    val x3 = table3.col('x)

//    val cnd = JoinCond.Eq(a1, first2)
//    val test = SHNrm(table1.repr, cnd , SHNrm(table2.repr, JoinCond.NoCond, SE))

    val joined1 = table1 innerJoin table2 on a1 <==> first2







//    val b = table.col('b)
//    val c = table.col('c)
//
//    val all = select(*) from table
//
//    val aOnly = select(a) from table
//
//    println(all.sql)
//    println(aOnly.sql)
//
//    val withWhere = {
//      select(*)
//        .from(table)
//        .where((a ==== 1) and (b like "abc%"))
//    }
//
//    println(withWhere.sql)
  }
}
