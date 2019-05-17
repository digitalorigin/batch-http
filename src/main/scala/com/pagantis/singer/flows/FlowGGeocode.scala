package com.pagantis.singer.flows

import java.time.ZoneId

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, StreamConverters}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Success

object FlowGGeocode extends App {

  val clazz = getClass.getName

  // init actor system, loggers and execution context
  implicit val system: ActorSystem = ActorSystem("GGeocodeFlow")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val standardLogger: LoggingAdapter = Logging(system, clazz)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val singerAdapter = SingerAdapter.fromConfig
  val ggcodeFlow = GGeocode.fromConfig

  val timeExtracted = java.time.LocalDateTime.now().atZone(ZoneId.of("UTC"))

  StreamConverters
    .fromInputStream(() => System.in)
    .log(clazz)
    .mapConcat(_.utf8String.split("\n").toList)
    .log(clazz)
    .map(singerAdapter.parseRecord(_))
    .log(clazz)
    .via(ggcodeFlow)
    .log(clazz)
    .map{case (response, address) => singerAdapter.toSingerRecord(response, address, timeExtracted)}
    .log(clazz)
    .map(singerAdapter.toJsonString(_))
    .runForeach(println(_))
    .onComplete(res => {
        Http().shutdownAllConnectionPools.andThen { case _ =>
          materializer.shutdown
          system.terminate
        }
        res match {
          case Success(_) => sys.exit(0)
          case _ => sys.exit(1)
        }
      }
    )

  // block main thread forever
  // exit will be handled on the stream exit callback
  Await.ready(Future.never, Duration.Inf)

}
