package com.pagantis.singer.flows

import spray.json._
import spray.json.JsValue

case class SingerMessage(`type`: String = "RECORD", stream: String = "geocode", record: JsValue)

object JsonProtocol extends DefaultJsonProtocol {

  implicit object SingerMessagFormat extends RootJsonFormat[SingerMessage] {

    def write(message: SingerMessage) =
      JsObject(
        "type" -> JsString(message.`type`),
        "stream" -> JsString(message.stream),
        "record" -> message.record
      )

    def read(value: JsValue) = value.asJsObject.getFields("type", "stream", "record") match {
      case  Seq(JsString("RECORD"), JsString("geocode"), record) =>
        SingerMessage(record = record)
      case _ => deserializationError("Singer.io message expected")
    }

  }

}


import JsonProtocol._

object SingerMessage {

  def fromLine(line: String): SingerMessage = line.parseJson.convertTo[SingerMessage]

  def toLine(message: SingerMessage): String = message.toJson.compactPrint

}


