package com.pagantis.singer.flows.test

import com.pagantis.singer.flows.SingerMessage
import org.scalatest.{FlatSpec, Matchers}
import spray.json._
import spray.json.DefaultJsonProtocol

class TestSingerMessage extends FlatSpec with Matchers with DefaultJsonProtocol {

  val line = """{"type":"RECORD","stream":"geocode","record":{"address":"1600 Amphitheatre Parkway, Mountain View, CA"}}"""

  val singerMessage = SingerMessage(
    `type` = "RECORD",
    record = """{"address": "1600 Amphitheatre Parkway, Mountain View, CA"}""".parseJson
  )

  "SingerMessage" should "extract address from a Singer message" in {

    SingerMessage.fromLine(line) shouldBe singerMessage

  }

  "SingerMessage" should "convert message to String line" in {

    SingerMessage.toLine(singerMessage) shouldBe line

  }

}
