package com.kgavriliuk.repository.tables

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import com.kgavriliuk.repository.DataBase._
import slick.lifted.TableQuery
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import com.kgavriliuk.models.User
import slick.jdbc.meta.MTable


class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("name")
  def password = column[String]("password")
  def role = column[String]("role")
  def * = (username, password, role) <> (User.tupled, User.unapply)
}

object UserDAO extends TableQuery(new Users(_)) {
  val table: TableQuery[Users] = TableQuery[Users]

  def checkByCredentials(username: String, password: String): Future[Option[String]] = {
    db.run(table.filter(user => user.username === username && user.password === password).map(user => user.role).result.headOption)
  }

  def tableExist: Boolean = {
    Await.result(db.run(MTable.getTables("users")), 3.seconds).toList.map(_.tableType)
      .contains("TABLE")
  }

  def dropCmd = DBIO.seq(table.schema.drop)

  def setup = Future {
    db.run(DBIO.seq(
      table.schema.create,
      table += User("user1234", "password1234", "user"),
      table += User("admin1234", "password1234", "admin")
    ))
  }

  def initUser = {
    if (tableExist) db.run(dropCmd).flatMap(_ => setup) else setup
  }
}
