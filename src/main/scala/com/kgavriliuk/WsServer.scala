package com.kgavriliuk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.kgavriliuk.repository.DataBase
import com.kgavriliuk.routes.WsRouter
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn

object WsServer extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()

  private val wsRoute = WsRouter()

  private val interface: String = config.getString("ws.interface")
  private val port: Integer = config.getInt("ws.port")

  DataBase.dbSetup
    .map(_ => bind)
    .foreach(bindRes => terminationHandler(bindRes))

  def bind = Http().bindAndHandle(wsRoute.route, interface, port)

  def terminationHandler(bind: Future[Http.ServerBinding]): Unit = {
    println(s"Started server at $interface:$port, press enter to kill server")
    StdIn.readLine()
    bind.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}
