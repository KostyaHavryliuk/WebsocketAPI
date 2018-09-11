package com.kgavriliuk.models

import java.util.UUID

import akka.actor.ActorRef

sealed trait Request
sealed trait Response

case class Connected(id: UUID, outgoing: ActorRef)
case class Disconnected(id: UUID)

case class IncomingMessage(id: UUID, request: Request) extends Request
case class OutgoingMessage(json: String) extends Response

case class Table_(id: Int, name: String, participants: Int)
case class User(username:String, password:String, role: String)

case class LoginRequest(username:String, password:String) extends Request
case class Ping(seq: Int) extends Request
case class Subscribe() extends Request
case class UnSubscribe() extends Request
case class JsonError(id: UUID, text: String) extends Request
case class AddTable(table: Table_) extends Request
case class UpdateTable(table: Table_) extends Request
case class RemoveTable(id: Int) extends Request

case class Pong(seq: Int) extends Response
case class TableAdded(table: Table_) extends Response
case class TableList(tables: List[Table_]) extends Response
case class TableUpdated(table: Table_) extends Response
case class TableDeleted(id: Int) extends Response
case class UpdateFailed(id: Int) extends Response
case class TableRemoveFailed(id: Int) extends Response
case class LoginFailed() extends Response
case class LoginSuccessful(user_type: String) extends Response
case class NotAuthorized() extends Response
case class WrongJSON(sentJSON: String) extends Response
case class Error(text: String) extends Response
