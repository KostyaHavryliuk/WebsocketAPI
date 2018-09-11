package com.kgavriliuk.actors

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import com.kgavriliuk.repository.tables.UserDAO._
import com.kgavriliuk.repository.tables.TableDAO._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class RequestActor() extends Actor {

  import com.kgavriliuk.models._

  class Client(val actor: ActorRef, var isAuthorized: Boolean = false, var isAdmin: Boolean = false, var isSubscribed: Boolean = false)

  val clientConnections: scala.collection.mutable.Map[UUID, Client] = scala.collection.mutable.Map[UUID, Client]()


  def receive = {
    case IncomingMessage(id, request) => requestHandler(id, request);
    case JsonError(id, text) => clientConnections(id).actor ! WrongJSON(text + s" - has wrong format, please check")
    case Connected(id, connectionActor) =>
      clientConnections += (id -> new Client(connectionActor))
      sender() ! clientConnections
    case Disconnected(connectionId) => clientConnections -= connectionId
    case msg => context.system.log.warning("receive unexpected message: " + msg)
  }

  def requestHandler(id: UUID, request: Request) = {
    request match {
      case user: LoginRequest => loginUser(user.username:String, user.password:String, id)
      case ping: Ping => pingUser(ping.seq, id: UUID)
      case _: Subscribe => subscribeUser(id: UUID)
      case _: UnSubscribe => unSubscribeUser(id: UUID)
      case addTable: AddTable => addTableToServer(id, addTable.table)
      case updateTable: UpdateTable => updateTableOnServer(id, updateTable.table)
      case removeTable: RemoveTable => deleteTable(id, removeTable.id)
      case _ =>
    }
  }

  def loginUser(username:String, password:String, id: UUID) = {
    checkByCredentials(username, password).onComplete {
      case Success(Some(user_role)) =>
        if (user_role == "user") clientConnections(id).isAuthorized = true
        else if (user_role == "admin") {
          clientConnections(id).isAdmin = true
          clientConnections(id).isAuthorized = true
        }
        clientConnections(id).actor ! LoginSuccessful(user_type = user_role)
      case _ => clientConnections(id).actor ! LoginFailed()
    }
  }

  def pingUser(seq:Int, id:UUID) = {
    clientConnections(id).actor ! Pong(seq = seq)
  }

  def subscribeUser(id: UUID) = {
    if(isAuthorized(id)) {
      clientConnections(id).isSubscribed = true
      getTableList.onComplete(tblList => clientConnections(id).actor ! TableList(tables = tblList.get))
    }
    else clientConnections(id).actor ! NotAuthorized()

  }

  def unSubscribeUser(id: UUID) = {
    if(isAuthorized(id)) {
      clientConnections(id).isSubscribed = false
    }
    else clientConnections(id).actor ! NotAuthorized()
  }

  def addTableToServer(id: UUID, table: Table_) = {
    if (isAdmin(id)) {
      addTable(table).onComplete {
        case Success(_) => clientConnections(id).actor ! TableAdded(table = Table_(table.id, table.name, table.participants))
          notifyUsers(TableAdded(table = Table_(table.id, table.name, table.participants)))
        case _ => clientConnections(id).actor ! Error("table with id: " + table.id + " already exist")
        }
      }
    else clientConnections(id).actor ! NotAuthorized()
  }

  def updateTableOnServer(id: UUID, table: Table_) = {
    if (isAdmin(id))
        updateTable(table).onComplete {
          case Success(1) => clientConnections(id).actor ! TableUpdated(table = table)
            notifyUsers(TableUpdated(table = table))
          case _ => clientConnections(id).actor ! UpdateFailed(id = table.id)
        }
    else clientConnections(id).actor ! NotAuthorized()

  }

  def deleteTable(id: UUID, tableId: Int) = {
    if (isAdmin(id)) {
      removeTable(tableId).onComplete {
        case Success(1) => clientConnections(id).actor ! TableDeleted(id = tableId)
          notifyUsers(TableDeleted(id = tableId))
        case _ => clientConnections(id).actor ! TableRemoveFailed(id = tableId)
      }
    }
    else clientConnections(id).actor ! NotAuthorized()
  }

  def isAuthorized(id: UUID): Boolean = {
    clientConnections.get(id) match {
      case Some(client) => client.isAuthorized
      case _ => false
    }
  }

  def isAdmin(id: UUID): Boolean = {
    clientConnections.get(id) match {
      case Some(client) => client.isAuthorized && client.isAdmin
      case _ => false
    }
  }

  def notifyUsers(response: Response): Unit = {
    clientConnections.filter(_._2.isSubscribed).foreach(_._2.actor ! response)
  }
}
