package typed.sql

import org.scalatest.{FunSpec, Matchers}
import shapeless._
import shapeless.test.illTyped
import typed.sql.syntax._

class TestSyntax extends FunSpec with Matchers{

  case class TestRow(
    a: Int,
    b: String,
    c: Boolean
  )
  val table1 = Table.of[TestRow].autoColumn('a).name('my_table)
  val a1 = table1.col('a)
  val b1 = table1.col('b)

  describe("delete") {

    it("delete") {
      delete.from(table1).astData shouldBe ast.Delete("my_table", None)
    }

    it("delete where") {
      val exp = ast.Delete("my_table", Some(ast.WhereEq(ast.Col("my_table", "a"))))
      delete.from(table1).where(a1 ==== 2).astData shouldBe exp
    }
  }

  describe("update") {

    it("update column") {
      val x = update(table1).set(b1 := "yoyo")
      x.astData shouldBe ast.Update("my_table", List(ast.Set(ast.Col("my_table", "b"))), None)
    }

    it("can't update primary key") {
      illTyped{"update(table1).set(a1 := 42)"}
    }

    it("with where") {
      val x = update(table1).set(b1 := "yoyo").where(a1 ==== 4)
      x.astData shouldBe ast.Update("my_table", List(ast.Set(ast.Col("my_table", "b"))), Some(ast.WhereEq(ast.Col("my_table", "a"))))
    }
  }

  describe("insert into") {

    it("insert all") {
      val x = insert.into(table1).values("b_value", "c_value")
      val data = x.astData

      val exp = ast.InsertInto("my_table", List(ast.Col("my_table", "b"), ast.Col("my_table", "c")))
      data shouldBe exp

      x.in shouldBe ("b_value" :: "c_value" :: HNil)
    }
  }
}