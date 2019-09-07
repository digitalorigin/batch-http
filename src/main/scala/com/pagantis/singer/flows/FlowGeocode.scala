package com.pagantis.singer.flows

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.StreamConverters
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

object FlowGeocode extends App {

  val clazz = getClass.getName

  // init actor system, loggers and execution context
  implicit val system: ActorSystem = ActorSystem("Geocode")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val standardLogger: LoggingAdapter = Logging(system, clazz)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()

  val endpoint = config.getString("flow.endpoint")
  val parallelism = config.getInt("flow.parallelism")

  val startTime = System.nanoTime()

  def parseResponse(triedResponse: (Try[HttpResponse], Address))(implicit am: Materializer) =
    triedResponse match {
      case (Success(response), address) =>
        Geocode.fromHttpResponse(response).map(_.toSingerMessage(address))
      case (Failure(exception), address) => {
        throw exception
      }
    }

  // This shutdown sequence was copied from another related issue: https://github.com/akka/akka-http/issues/907#issuecomment-345288919
  def shutdownSequence =
    Http().shutdownAllConnectionPools

  val flowComputation =
    StreamConverters
      .fromInputStream(() => System.in)
      .log(clazz)
      .mapConcat(_.utf8String.split("\n").toList)
      .log(clazz)
      .map(SingerMessage.fromLine)
      .log(clazz)
      .map(
        singerMessage => {
          val address = Address.fromSingerMessage(singerMessage)
          (address.toHttpRequest, address)
        }
      )
      .log(clazz)
      .log(clazz)
      .via(Http().cachedHostConnectionPoolHttps[Address](host = endpoint))
      .log(clazz)
      .mapAsync(parallelism)(parseResponse(_))
      .map(SingerMessage.toLine)
      .log(clazz)
      .runForeach(println(_))

  Await.ready(flowComputation, Duration.Inf)

  standardLogger.info(s"Total execution time: ${(System.nanoTime - startTime)/1000000000} seconds")

  Await.ready(shutdownSequence, Duration.Inf)

  flowComputation.value match {
    case Some(Success(_)) => sys.exit(0)
    case _ => sys.exit(1)
  }

}
