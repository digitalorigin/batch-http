package com.pagantis.singer.flows

import java.net.URLEncoder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe
import com.typesafe.config.ConfigFactory
import spray.json.JsValue

import scala.concurrent.ExecutionContext
import scala.util.Try

object GGeocode {
  def fromConfig(implicit ac : ActorSystem, am : ActorMaterializer, ec : ExecutionContext) : Flow[AddressWrapper, (JsValue, AddressWrapper), NotUsed] = {
    val config = ConfigFactory.load().getConfig("flow")
    val key = config.getString("api_key")
    val endpoint = config.getString("endpoint")
    val parallelism = config.getInt("parallelism")
    new GGeocode(key).flow(endpoint, parallelism)
  }
}

class GGeocode(apiKey : String) {

  def flow(endpoint: String, parrallelism : Int)(implicit ac : ActorSystem, am : ActorMaterializer, ec : ExecutionContext) = {
    val poolClientFlow = Http().cachedHostConnectionPoolHttps[AddressWrapper](host = endpoint)
    Flow[AddressWrapper]
      .map(createGoogleMapsRequest(_))
      .via(poolClientFlow)
      .map{case (attempt, address) => parseGGeoResponseAttempt(attempt, address)}
      .mapAsync(parrallelism)(p => p)
  }

  private [flows] def createGoogleMapsRequest(addressWrapper : AddressWrapper) = {
    HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(s"/maps/api/geocode/json?region=es&language=es&address=${URLEncoder.encode(addressWrapper.address.simpleStringRepresentation, "UTF-8")}&key=$apiKey")
    ) -> addressWrapper
  }

  private def parseGGeoResponse(response: HttpResponse, addressWrapper: AddressWrapper)(implicit am : Materializer, ec : ExecutionContext) = {
    import spray.json._
    (response.entity.dataBytes.runFold(ByteString(""))(_ ++ _) map
      (body => body.utf8String.parseJson)).map(_ -> addressWrapper)
  }
  private def parseGGeoResponseAttempt(response: Try[HttpResponse], addressWrapper: AddressWrapper)(implicit am : Materializer, ex: ExecutionContext) =
    parseGGeoResponse(response.get, addressWrapper)

}
