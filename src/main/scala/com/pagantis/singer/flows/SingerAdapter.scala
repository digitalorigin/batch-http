package com.pagantis.singer.flows

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait SingerMessage {
  def asJsonString: String
}

case class GGeoRecord(
                            `type`: String = "RECORD",
                            stream: String =  "ggeocoding",
                            time_extracted: ZonedDateTime,
                            address_id: Option[String],
                            record: JsValue
                          )

case class RawAddress(address: String, city: Option[String], zip_code: Option[String]) {
  def simpleStringRepresentation = List(
    Some(address),
    city,
    zip_code
  ) collect {case Some(value) => value } mkString(",")
}
case class Address(street: String, nr: Option[String], city: Option[String], zip_code: Option[String]) {
  def asRawAddress = RawAddress(
    if(nr.isDefined) s"$street ${nr.get}" else street,
    city,
    zip_code
  )
}
case class AddressWrapper(addressId: Option[String], address: RawAddress)

// json protocol
object JsonProtocol extends DefaultJsonProtocol {
  implicit object DateJsonFormat extends RootJsonFormat[ZonedDateTime] {
    override def write(datetime: ZonedDateTime): JsValue = {
      JsString(datetime.format(DateTimeFormatter.ISO_INSTANT))
    }
    //noinspection NotImplementedCode
    override def read(json: JsValue): ZonedDateTime = ??? //TODO: we won't read dates in the stream for now
  }
  implicit val recordSerde: RootJsonFormat[GGeoRecord] = jsonFormat5(GGeoRecord)
  implicit val rawAddressSerde: RootJsonFormat[RawAddress] = jsonFormat3(RawAddress)
  implicit val addressSerde: RootJsonFormat[Address] = jsonFormat4(Address)
}

object SingerAdapter {
  def fromConfig =
    new SingerAdapter
}

class SingerAdapter {
  import JsonProtocol._
  import spray.json._

  def toSingerRecord(jsValue: JsValue, addressWrapper: AddressWrapper, timeExtracted: ZonedDateTime) = {
    GGeoRecord(
      time_extracted = timeExtracted,
      address_id = addressWrapper.addressId,
      record = jsValue
    )
  }

  def parseRecord(line: String) = {
    val jsonLine = line.parseJson.asJsObject
    val address = jsonLine.fields("stream") match {
      case JsString("raw_address") =>
        line.parseJson.asJsObject.fields("record").convertTo[RawAddress]
      case JsString("normalized_address") =>
        line.parseJson.asJsObject.fields("record").convertTo[Address].asRawAddress
    }
    val addressId = jsonLine.fields.get("address_id") match {
      case Some(JsString(id)) => Some(id)
      case None => None
    }
    AddressWrapper(addressId, address)
  }

  def toJsonString(ggeoRecord: GGeoRecord): String =
    ggeoRecord.toJson.compactPrint

}


