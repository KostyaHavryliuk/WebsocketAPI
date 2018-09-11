package com.kgavriliuk.repository

import slick.jdbc.PostgresProfile.api._
import com.kgavriliuk.repository.tables.UserDAO._
import com.kgavriliuk.repository.tables.TableDAO._
import scala.concurrent.ExecutionContext.Implicits.global

object DataBase {
  val db = Database.forConfig("postgresDB")

  def dbSetup = {
    initUser.flatMap(_ => initTable)
  }
}
