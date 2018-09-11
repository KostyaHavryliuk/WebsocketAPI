package com.kgavriliuk

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.kgavriliuk.actors.RequestActor
import com.kgavriliuk.models._

import scala.reflect.ClassTag


trait ActorSpec {
  implicit lazy val system: ActorSystem = ActorSystem(getClass.getSimpleName)

  abstract class ActorScope extends TestKit(system) with ImplicitSender with DefaultTimeout {

    val requestActor = {
      def actor() = new RequestActor()

      val props = Props(actor())
      system.actorOf(props)
    }

    def expectResponse[T <: Response](implicit classTag: ClassTag[T]): T = expectMsgType[T]

    class Client(val actor: ActorRef, var isAuthorized: Boolean = false, var isAdmin: Boolean = false, var isSubscribed: Boolean = false)

    val connectionId = UUID.randomUUID()
    val tableList = List(Table_(1, "table - James Bond", 7), Table_(2, "table - Mission Impossible", 4))
    val table = Table_(2, "table - Test table", 5)
    val table1 = Table_(3, "table - Add table", 5)
    val wrongTable = Table_(34, "unknown", 100)

  }

}
