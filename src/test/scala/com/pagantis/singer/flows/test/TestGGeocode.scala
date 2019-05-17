package com.pagantis.singer.flows.test

import java.net.URLEncoder
import java.time.ZoneId

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import com.pagantis.singer.flows.{AddressWrapper, GGeocode, RawAddress, SingerAdapter}
import org.scalatest.{FlatSpec, Matchers}

class TestGGeocode extends FlatSpec with Matchers {

  val singerAdapter = new SingerAdapter
  val someZonedDatetime = java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))

  val ggeocode = new GGeocode("somekey")

  "GGeocode" should "create request from Address" in {

    val rwAddress =  RawAddress("Pazos Fontenla 80, 3A", Some("Bueu"), Some("36930"))

    ggeocode.createGoogleMapsRequest(
      AddressWrapper(None, rwAddress)
    ) shouldBe (
      HttpRequest(
        method = HttpMethods.GET,
        uri = s"/maps/api/geocode/json?region=es&language=es&address=${URLEncoder.encode("Pazos Fontenla 80, 3A,Bueu,36930")}&key=somekey"
      ) -> AddressWrapper(None, rwAddress)
      )

  }

}
