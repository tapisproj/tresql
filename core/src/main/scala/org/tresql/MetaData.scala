package org.tresql

import java.util.NoSuchElementException
import sys._

/** Implementation of meta data must be thread safe */
trait MetaData {
  import metadata._
  def join(table1: String, table2: String): (key_, key_) = {
    val t1 = table(table1); val t2 = table(table2)
    (t1.refs(t2.name), t2.refs(t1.name)) match {
      case (k1, k2) if (k1.length + k2.length > 1) =>
        val r1 = reduceRefs(k1, t2.key)
        val r2 = reduceRefs(k2, t1.key)
        if (r1.length + r2.length == 1)
          if (r1.length == 1) (fk(r1.head.cols), uk(r1.head.refCols)) else (uk(r2.head.refCols), fk(r2.head.cols))
        else if (r1.length == 1) (fk(r1.head.cols), uk(r1.head.refCols))
        else error("Ambiguous relation. Too many found between tables " + table1 + ", " + table2)
      case (k1, k2) if (k1.length + k2.length == 0) => { //try to find two imported keys of the same primary key
        t1.rfs.filter(_._2.size == 1).foldLeft(List[(List[String], List[String])]()) {
          (res, t1refs) =>
              t2.rfs.foldLeft(res)((r, t2refs) => if (t2refs._2.size == 1 && t1refs._1 == t2refs._1)
                (t1refs._2.head.cols -> t2refs._2.head.cols) :: r else r)
        } match {
          case Nil => error("Relation not found between tables " + table1 + ", " + table2)
          case List((a, b)) => (fk(a), fk(b))
          case b => error("Ambiguous relation. Too many found between tables " + table1 + ", " +
            table2 + ". Relation columns: " + b)
        }
      }
      case (k1, k2) =>
        if (k1.length == 1) (fk(k1.head.cols), uk(k1.head.refCols)) else (uk(k2.head.refCols), fk(k2.head.cols))
    }
  }

  private def reduceRefs(refs: List[Ref], key: Key) = {
    def importedPkKeyCols(ref: Ref, key: Key) = {
      val refsPk = ref.cols zip ref.refCols
      key.cols.foldLeft(Option(List[String]())) {
        (importedKeyCols, keyCol) =>
          importedKeyCols.flatMap(l => refsPk.find(_._2 == keyCol).map(_._1 :: l))
      } map (_.reverse)
    }
    
    refs.groupBy(importedPkKeyCols(_, key)) match {
      case m if m.size == 1 && m.head._1.isDefined => List(refs.minBy(_.cols.size))
      case _ => refs
    }
  }

  def col(table: String, col: String): Col = this.table(table).col(col)
  def colOption(table: String, col: String): Option[Col] = this.tableOption(table).flatMap(_.colOption(col))
  def col(col: String): Col =
    table(col.substring(0, col.lastIndexOf('.'))).col(col.substring(col.lastIndexOf('.') + 1))
  def colOption(col: String): Option[Col] = tableOption(col.substring(0, col.lastIndexOf('.'))).flatMap(
    _.colOption(col.substring(col.lastIndexOf('.') + 1)))

  def table(name: String): Table
  def tableOption(name: String): Option[Table]
  def procedure(name: String): Procedure
  def procedureOption(name: String): Option[Procedure]
}

//TODO pk col storing together with ref col (for multi col key secure support)?
package metadata {
  case class Table(name: String, cols: List[Col], key: Key,
      rfs: Map[String, List[Ref]]) {
    private val colMap = cols map (c => c.name -> c) toMap
    val refTable: Map[List[String], String] = rfs.flatMap(t => t._2.map(_.cols -> t._1))
    def col(name: String) = colMap(name)
    def colOption(name: String) = colMap.get(name)
    def refs(table: String) = rfs.get(table).getOrElse(Nil)
    def ref(table: String, ref: List[String]) = rfs(table)
    .find(_.cols == ref)
    .getOrElse(error(s"""Column(s) "$ref" is not reference from table "$name" to table "$table" """))
  }
  object Table {
    def apply(t: Map[String, Any]): Table = {
      Table(t("name").toString.toLowerCase, t("cols") match {
        case l: List[Map[String, String]] => l map { c =>
          Col(c("name").toString.toLowerCase,
            c("nullable").asInstanceOf[Boolean])
        }
      }, t("key") match { case l: List[String] => Key(l map (_.toLowerCase)) }, t("refs") match {
        case l: List[Map[String, Any]] => (l map { r =>
          (r("table").asInstanceOf[String].toLowerCase,
            r("refs") match {
              case l: List[List[(String, String)]] => l map (rc => {
                  val t = rc.unzip
                  Ref(t._1 map (_.toLowerCase), t._2 map (_.toLowerCase))
              })
            })
        }).toMap
      })
    }
  }
  case class Col(name: String, nullable: Boolean)
  case class Key(cols: List[String])
  case class Ref(cols: List[String], refCols: List[String])
  case class Procedure(name: String, comments: String, procType: Int,
    pars: List[Par], returnSqlType: Int, returnTypeName: String)
  case class Par(name: String, comments: String, parType: Int, sqlType: Int, typeName: String)
  trait key_ { def cols: List[String] }
  case class uk(cols: List[String]) extends key_
  case class fk(cols: List[String]) extends key_
}
