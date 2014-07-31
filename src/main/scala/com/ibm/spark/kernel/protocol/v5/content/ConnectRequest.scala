package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json._

case class ConnectRequest()

object ConnectRequest{
  private val SingleInstance = ConnectRequest()
  private val EmptyJsonObj = Json.obj()

  implicit val connectRequestReads = new Reads[ConnectRequest] {
    override def reads(json: JsValue): JsResult[ConnectRequest] = {
      new JsSuccess[ConnectRequest](SingleInstance)
    }
  }

  implicit val connectRequestWrites = new Writes[ConnectRequest] {
    override def writes(req: ConnectRequest): JsValue = EmptyJsonObj
  }
}
