package com.kgavriliuk.serializers

import com.kgavriliuk.models._
import org.json4s
import org.json4s.{DefaultFormats, TypeHints}
import org.json4s.jackson.Serialization._

import scala.util.Try

object JsonFormats {

  case class CustomHints(customHintsMap: Map[String, Class[_]]) extends TypeHints {
    val hints: List[Class[_]] = customHintsMap.values.toList

    def keyForValue(map: Map[String, Class[_]], value: Class[_]) = {
      val revMap = map map {_.swap}
      val key = revMap(value)
      key
    }

    override def hintFor(clazz: Class[_]): String = keyForValue(customHintsMap, clazz)

    override def classFor(hint: String): Option[Class[_]] = customHintsMap.get(hint)
  }

  val customHints = CustomHints(
    Map(
      "login" -> classOf[LoginRequest],
      "ping" -> classOf[Ping],
      "subscribe_tables" -> classOf[Subscribe],
      "unsubscribe_tables" -> classOf[UnSubscribe],
      "add_table" -> classOf[AddTable],
      "update_table" -> classOf[UpdateTable],
      "remove_table" -> classOf[RemoveTable],
      "pong" -> classOf[Pong],
      "table_added" -> classOf[TableAdded],
      "table_list" -> classOf[TableList],
      "table_updated" -> classOf[TableUpdated],
      "table_removed" -> classOf[TableDeleted],
      "update_failed" -> classOf[UpdateFailed],
      "remove_failed" -> classOf[TableRemoveFailed],
      "login_successful" -> classOf[LoginSuccessful],
      "login_failed" -> classOf[LoginFailed],
      "not_authorized" -> classOf[NotAuthorized],
      "error" -> classOf[Error]
    )
  )

  implicit val formats: json4s.Formats = new DefaultFormats {
    override val typeHintFieldName = "$type"
  } + customHints
}

trait JsonFormats{
  import JsonFormats._

  def deserialize(txt: String): Request = {
    read[Request](txt)
  }

  def serialize(response: Response): String = {
    write(response)
  }
}
