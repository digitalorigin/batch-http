package com.pagantis.singer.flows.test

import com.pagantis.singer.flows.{GetRequest, PostRequest, Request, SingerMessage}
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{DefaultJsonProtocol, JsObject, JsString}

class TestRequest extends FlatSpec with Matchers with DefaultJsonProtocol {

  val singerGet = SingerMessage(
    record = JsObject(
      "query" -> JsObject(
        "parm1" -> JsString("value1"),
        "parm2" -> JsString("value2")
      )
    )
  )

  val singerPost = SingerMessage(
    record = JsObject(
      "body" -> JsObject(
        "parm1" -> JsString("value1"),
        "parm2" -> JsString("value2")
      )
    )
  )

  "Request" should "create GET request when a query map is passed" in {

    Request.fromSingerMessage(singerGet) shouldBe a[GetRequest]

  }

  "Request" should "create POST request when a body is passed" in {

    Request.fromSingerMessage(singerPost) shouldBe a[PostRequest]

  }

}
