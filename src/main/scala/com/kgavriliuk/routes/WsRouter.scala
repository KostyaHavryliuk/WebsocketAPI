package com.kgavriliuk.routes

import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.kgavriliuk.actors.RequestActor
import com.kgavriliuk.models._
import com.kgavriliuk.serializers.JsonFormats

import scala.util.Try


case class WsRouter(implicit system: ActorSystem,
                    implicit val materializer: ActorMaterializer) extends Directives with JsonFormats {


  val route = path("socket") {
    get {
      handleWebSocketMessages(webSocketMessagesFlow)
    }
  }

  val playerActor = system.actorOf(Props(new RequestActor()), "playersActor")

  private def webSocketMessagesFlow: Flow[Message, Message, NotUsed] = {
    val connectionId = UUID.randomUUID()


    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        // transform websocket message to domain message
        case TextMessage.Strict(text) => Try(IncomingMessage(connectionId, deserialize(text))).getOrElse(JsonError(connectionId, text))
      }.to(Sink.actorRef(playerActor, Disconnected(connectionId)))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef(10, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          // give the user actor a way to send messages out
          playerActor ! Connected(connectionId, outActor)
          NotUsed
        }.map(

        (outMsg: Response) => TextMessage(serialize(outMsg)))


    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
