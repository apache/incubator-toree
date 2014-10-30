package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.json._

case class KernelInfoRequest()

object KernelInfoRequest{
  private val SingleInstance = KernelInfoRequest()
  private val EmptyJsonObj = Json.obj()

  implicit val kernelInfoRequestReads = new Reads[KernelInfoRequest] {
    override def reads(json: JsValue): JsResult[KernelInfoRequest] = {
      new JsSuccess[KernelInfoRequest](SingleInstance)
    }
  }

  implicit val kernelInfoRequestWrites = new Writes[KernelInfoRequest] {
    override def writes(req: KernelInfoRequest): JsValue = EmptyJsonObj
  }
}
