package com.kgavriliuk.repository.tables

import akka.http.scaladsl.model.headers.CacheDirectives.public
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import com.kgavriliuk.repository.DataBase._
import slick.lifted.TableQuery

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import com.kgavriliuk.models.Table_
import slick.jdbc.meta.{MQName, MTable}


class Tables(tag: Tag) extends Table[Table_](tag, "tables") {
  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  def participants = column[Int]("participants")
  def * = (id, name, participants) <> (Table_.tupled, Table_.unapply)
}

object TableDAO extends TableQuery(new Tables(_)) {
  val table: TableQuery[Tables] = TableQuery[Tables]

  def addTable(newTable: Table_): Future[_] = {
    db.run(DBIO.seq(table += newTable))
  }

  def updateTable(newTable: Table_): Future[Int] = {
    db.run(table.filter(_.id === newTable.id).map(table => (table.id, table.name, table.participants)).update(newTable.id, newTable.name, newTable.participants))
  }

  def removeTable(id: Int): Future[Int] = {
    db.run(table.filter(_.id === id).delete)
  }

  def getTableList: Future[List[Table_]] = {
    db.run(table.to[List].result)
  }

  def tablesExist: Boolean = {
    Await.result(db.run(MTable.getTables("tables")), 3.seconds).toList.map(_.tableType)
      .contains("TABLE")

  }

  def dropCmd = {
    DBIO.seq(table.schema.drop)
  }

  def setup = Future {
    db.run(DBIO.seq(
      table.schema.create,
      table += Table_(1, "table - James Bond", 7),
      table += Table_(2, "table - Mission Impossible", 4)
    ))
  }

  def initTable = {
    if(tablesExist) db.run(dropCmd).flatMap(_ => setup) else setup
  }
}
