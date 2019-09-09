package com.pagantis.singer.flows

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.stream.Materializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import spray.json.{JsObject, JsString, JsValue}

object Request {

  val config = ConfigFactory.load()

  val extraParams = config.as[Map[String,String]]("flow.extra_params")
  val defaultPath = config.getString("flow.path")

  def fromSingerMessage(message: SingerMessage) = {

    val fields = message.record.asJsObject.fields

    (fields.get("query"), fields.get("body")) match {

      case (Some(_), Some(_)) =>
        throw new Exception

      case (Some(jsonQueryParams), None) =>
        val asStringMap = jsonQueryParams
            .asJsObject
            .fields
            .map{
              case (key, JsString(value)) => (key, value)
            }
        GetRequest(asStringMap ++ extraParams)

      case (None, Some(jsonBody)) =>
        PostRequest(jsonBody.asJsObject)
    }

  }

  def fromHttpResponse(response: HttpResponse)(implicit am: Materializer) = {

    import spray.json._
    implicit val ec = am.executionContext

    val responseAsJson = response.entity.dataBytes.runFold(ByteString(""))(_ ++ _) map
      (body => body.utf8String.parseJson)


    responseAsJson

  }

}


trait Request {

  def toAkkaRequest: HttpRequest

  def toSingerMessage(response: JsValue): SingerMessage

}

case class GetRequest(queryParams: Map[String, String]) extends Request {

  override def toAkkaRequest: HttpRequest = {
    val query = Query(queryParams)
    HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(Request.defaultPath).withQuery(query)
    )
  }

  override def toSingerMessage(response: JsValue): SingerMessage = {

    val noSecrets = queryParams.filter(param => !Request.extraParams.contains(param._1))

    SingerMessage(
      record = JsObject(
        "request" -> JsObject(noSecrets.map{ case (key, value) => (key, JsString(value))}),
        "response" -> response
      )
    )
  }

}

case class PostRequest(body: JsObject) extends Request {

  override def toAkkaRequest: HttpRequest = ???

  override def toSingerMessage(response: JsValue): SingerMessage = ???

}
