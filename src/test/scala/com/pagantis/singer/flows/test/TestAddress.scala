package com.pagantis.singer.flows.test

import com.pagantis.singer.flows.{Address, SingerMessage}
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{DefaultJsonProtocol, _}

class TestAddress extends FlatSpec with Matchers with DefaultJsonProtocol {

  val singerMessage = SingerMessage(record = """{"address": "1600 Amphitheatre Parkway","city": "Mountain View","zipcode": "9090"}""".parseJson)

  val address = Address(
    address = "1600 Amphitheatre Parkway",
    city = "Mountain View",
    zipcode = "9090"
  )

  "Address" should "convert from SingerMessage" in {

    Address.fromSingerMessage(singerMessage) shouldBe address

  }

  "Address" should "simple string format" in {

    address.asString shouldBe "1600 Amphitheatre Parkway,Mountain View,9090"

  }

  "Address" should "encode as URL" in {

    address.urlEncoded shouldBe "1600+Amphitheatre+Parkway%2CMountain+View%2C9090"
  }

}
