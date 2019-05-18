package com.pagantis.singer.flows.test

import java.time.ZoneId

import com.pagantis.singer.flows.{AddressWrapper, GGeoRecord, GGeocode, RawAddress, SingerAdapter}
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{JsObject, JsString}

class TestSingerAdapter extends FlatSpec with Matchers {

  val singerAdapter = new SingerAdapter
  val someZonedDatetime = java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC"))
  val someAddress = RawAddress(
    address = "Paseo Circumvalación 39 2º3ª",
    city = Some("Gelida"),
    zip_code = Some("08790")
  )

  "SingerAdapter" should "parse a valid input raw address as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "raw_address",
        | "time_extracted": null,
        | "record": {
        |   "address": "Paseo Circumvalación 39 2º3ª",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        None,
        RawAddress(
          address = "Paseo Circumvalación 39 2º3ª",
          city = Some("Gelida"),
          zip_code = Some("08790")
        )
      )
  }

  "SingerAdapter" should "parse a valid input raw address with missing city as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "raw_address",
        | "time_extracted": null,
        | "record": {
        |   "address": "Paseo Circumvalación 39 2º3ª",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        None,
        RawAddress(
          address = "Paseo Circumvalación 39 2º3ª",
          city = None,
          zip_code = Some("08790")
        )
    )
  }

  "SingerAdapter" should "parse a valid input raw address with null city as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "raw_address",
        | "time_extracted": null,
        | "record": {
        |   "address": "Paseo Circumvalación 39 2º3ª",
        |   "city": null,
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        None,
        RawAddress(
          address = "Paseo Circumvalación 39 2º3ª",
          city = None,
          zip_code = Some("08790")
        )
    )
  }

  "SingerAdapter" should "fail when no address is provided" in {
    a [spray.json.DeserializationException] should be thrownBy (
      singerAdapter.parseRecord(
        """
          |{
          | "type": "RECORD",
          | "stream": "raw_address",
          | "time_extracted": null,
          | "record": {
          |   "address": null
          | }
          |}
        """.stripMargin)
      )
  }

  "SingerAdapter" should "parse a valid input normalized address as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "normalized_address",
        | "time_extracted": null,
        | "record": {
        |   "street": "Paseo Circumvalación",
        |   "nr": "39",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        None,
        RawAddress(
          address = "Paseo Circumvalación 39",
          city = Some("Gelida"),
          zip_code = Some("08790")
        )
    )
  }

  "SingerAdapter" should "parse a valid input normalized address with missing number as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "normalized_address",
        | "time_extracted": null,
        | "record": {
        |   "street": "Paseo Circumvalación",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        None,
        RawAddress(
          address = "Paseo Circumvalación",
          city = Some("Gelida"),
          zip_code = Some("08790")
        )
      )
  }

  "SingerAdapter" should "parse a valid input normalized address with id as an Address" in {
    singerAdapter.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "normalized_address",
        | "time_extracted": null,
        | "address_id": "someid",
        | "record": {
        |   "street": "Paseo Circumvalación",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
        Some("someid"),
        RawAddress(
          address = "Paseo Circumvalación",
          city = Some("Gelida"),
          zip_code = Some("08790")
        )
      )
  }

  "SingerAdapter" should "convert a GMaps response to a GGeocode" in  {
    singerAdapter.toSingerRecord(
      JsObject(),
      AddressWrapper(None, someAddress),
      someZonedDatetime
    ) shouldBe GGeoRecord(
      time_extracted = someZonedDatetime,
      record = JsObject(),
      address_id = None
    )
  }

  "SingerAdapter" should "convert a GGeocode to a valid Singer.io message" in {
    singerAdapter.toJsonString(
      GGeoRecord(
        time_extracted = java.time.LocalDateTime.of(2019, 4, 23, 12, 0, 0).atZone(ZoneId.of("UTC")),
        record = JsObject(
          "some_key" -> JsString("some_key")
        ),
        address_id = None
      )
    ) shouldBe """{"type":"RECORD","stream":"ggeocoding","time_extracted":"2019-04-23T12:00:00Z","record":{"some_key":"some_key"}}"""
  }

  "SingerAdapter" should "map record keys when mapping configuration is provided" in {
    val singerAdapterWithMapping = new SingerAdapter(Map("$.address.path" -> "address"))
    singerAdapterWithMapping.applyMappings(
      JsObject(
        "$.address.path" -> JsString("Paseo Circumvalación 32"),
        "other_key" -> JsString("some value")
      )
    ) shouldBe JsObject(
      "address" -> JsString("Paseo Circumvalación 32"),
      "other_key" -> JsString("some value")
    )
  }
  "SingerAdapter" should "parse an Address with mappings" in {
    val singerAdapterWithMapping = new SingerAdapter(Map("$.address.path" -> "address"))
    singerAdapterWithMapping.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "raw_address",
        | "time_extracted": null,
        | "address_id": "someid",
        | "record": {
        |   "$.address.path": "Paseo Circumvalación 32",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
      Some("someid"),
      RawAddress(
        address = "Paseo Circumvalación 32",
        city = Some("Gelida"),
        zip_code = Some("08790")
      )
    )
  }

  "SingerAdapter" should "parse an Address with mappings and stream override" in {
    val singerAdapterWithMapping = new SingerAdapter(Map("$.address.path" -> "address"), Some("raw_address"))
    singerAdapterWithMapping.parseRecord(
      """
        |{
        | "type": "RECORD",
        | "stream": "other-stream-name",
        | "time_extracted": null,
        | "address_id": "someid",
        | "record": {
        |   "$.address.path": "Paseo Circumvalación 32",
        |   "city": "Gelida",
        |   "zip_code": "08790"
        | }
        |}
      """.stripMargin) shouldBe AddressWrapper(
      Some("someid"),
      RawAddress(
        address = "Paseo Circumvalación 32",
        city = Some("Gelida"),
        zip_code = Some("08790")
      )
    )
  }

}
