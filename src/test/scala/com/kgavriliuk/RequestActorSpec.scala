package com.kgavriliuk

import java.util.UUID

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import com.kgavriliuk.models._
import org.scalatest.concurrent.Eventually
import com.kgavriliuk.repository.tables.UserDAO._
import com.kgavriliuk.repository.tables.TableDAO._

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class RequestActorSpec extends WordSpec with Matchers with BeforeAndAfterAll with Eventually with ActorSpec {

  override def beforeAll(): Unit = {
    Await.result(initUser.flatMap(_ => initTable), 10.seconds)
  }

  "RequestActor" must  {

    "Answer with Pong when got Ping" in new ActorScope  {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, Ping(1))
      expectResponse[Pong] shouldEqual Pong(1)
    }

    "Answer with LoginSuccessful, when got valid credentials" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("user1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "user")
    }

    "Answer with LoginFailed, when got invalid credentials" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("hacker", "password1234"))
      expectResponse[LoginFailed] shouldEqual LoginFailed()
    }

    "Answer with TableList, when got subscribe request" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("user1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "user")

      requestActor ! IncomingMessage(connectionId, Subscribe())
      expectResponse[TableList] shouldEqual TableList(tables = tableList)
    }

    "Answer with TableAdded, when got AddTable request" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("admin1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "admin")

      requestActor ! IncomingMessage(connectionId, AddTable(table1))
      expectResponse[TableAdded] shouldEqual TableAdded(table1)
    }

    "Answer with NotAuthorized, when got AddTable request and logged as User" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("user1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "user")

      requestActor ! IncomingMessage(connectionId, AddTable(table1))
      expectResponse[NotAuthorized] shouldEqual NotAuthorized()
    }

    "Answer with TableUpdated, when got UpdateTable request" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("admin1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "admin")

      requestActor ! IncomingMessage(connectionId, UpdateTable(table))
      expectResponse[TableUpdated] shouldEqual TableUpdated(table = table)
    }

    "Answer with UpdateFailed, when got UpdateTable request for nonexistent Table" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("admin1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "admin")

      requestActor ! IncomingMessage(connectionId, UpdateTable(wrongTable))
      expectResponse[UpdateFailed] shouldEqual UpdateFailed(wrongTable.id)
    }

    "Answer with TableDeleted, when got RemoveTable request" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("admin1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "admin")

      requestActor ! IncomingMessage(connectionId, RemoveTable(table.id))
      expectResponse[TableDeleted] shouldEqual TableDeleted(table.id)
    }

    "Answer with TableRemoveFailed, when got RemoveTable request for nonexistent Table" in new ActorScope {
      requestActor ! Connected(connectionId, self)
      expectMsgType[mutable.Map[UUID, Client]]

      requestActor ! IncomingMessage(connectionId, LoginRequest("admin1234", "password1234"))
      expectResponse[LoginSuccessful] shouldEqual LoginSuccessful(user_type = "admin")

      requestActor ! IncomingMessage(connectionId, RemoveTable(wrongTable.id))
      expectResponse[TableRemoveFailed] shouldEqual TableRemoveFailed(wrongTable.id)
    }
  }
}