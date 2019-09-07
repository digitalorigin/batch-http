package com.pagantis.singer.flows

import java.net.URLEncoder

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import spray.json.{JsString, JsValue}


object Address {

  private val apiKey = ConfigFactory.load().getString("flow.api_key")

  def fromSingerMessage(message: SingerMessage) = {
    val fields = message.record.asJsObject.fields
    (fields("address"), fields("city"), fields("zipcode")) match {
      case (JsString(address), JsString(city), JsString(zipcode)) =>
        Address(address, city, zipcode)
    }
  }

}

case class Address(address: String, city: String, zipcode: String, others: Option[Map[String, String]] = None) {

  // address has to be built this way to maximize cache hits in cloudfront together with CRE team
  // https://github.com/digitalorigin/pmt-cre-collectors/blob/6b783b08bede460dac586c4fb172cca23d8b34e4/adapters/src/main/java/com/digitalorigin/cre/collector/adapter/addressNormalizer/GoogleApiAddressNormalizerClient.java#L31
  def asString =
    address + "," + city + "," + zipcode

  def toHttpRequest: HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = Uri(s"/maps/api/geocode/json?region=es&language=es&address=$urlEncoded&key=${Address.apiKey}")
  )

  def urlEncoded: String = URLEncoder.encode(asString,"UTF-8")

}

object Geocode {

  def fromHttpResponse(response: HttpResponse)(implicit am: Materializer) = {

    import spray.json._
    implicit val ec = am.executionContext

    val responseAsJson = response.entity.dataBytes.runFold(ByteString(""))(_ ++ _) map
      (body => body.utf8String.parseJson)


    responseAsJson.map(Geocode(_))

  }
}

case class Geocode(response: JsValue) {

  def toSingerMessage(context: Address) = {

    import spray.json._
    import JsonProtocol._

    val contextAsJson = context.toJson

      SingerMessage(
        record = JsObject(
          "address" -> contextAsJson,
          "geocode" -> response
        )
      )
  }

}
