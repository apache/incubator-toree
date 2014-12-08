package com.ibm.spark.kernel.protocol.v5

import java.nio.charset.Charset

import akka.util.{ByteString, Timeout}
import akka.zeromq.ZMQMessage
import com.ibm.spark.utils.LogLike
import play.api.data.validation.ValidationError
import play.api.libs.json.{Reads, JsPath, Json}
import scala.concurrent.duration._

object Utilities extends LogLike {
  //
  // NOTE: This is brought in to remove feature warnings regarding the use of
  //       implicit conversions regarding the following:
  //
  //       1. ByteStringToString
  //       2. ZMQMessageToKernelMessage
  //
  import scala.language.implicitConversions

  /**
   * This timeout needs to be defined for the Akka asks to timeout
   */
  implicit val timeout = Timeout(100000.days)

  implicit def ByteStringToString(byteString : ByteString) : String = {
    new String(byteString.toArray, Charset.forName("UTF-8"))
  }

  implicit def StringToByteString(string : String) : ByteString = {
    ByteString(string.getBytes)
  }

  implicit def ZMQMessageToKernelMessage(message: ZMQMessage): KernelMessage = {
    val delimiterIndex: Int =
      message.frames.indexOf(ByteString("<IDS|MSG>".getBytes))
    //  TODO Handle the case where there is no delimeter
    val ids: Seq[String] =
      message.frames.take(delimiterIndex).map(
        (byteString : ByteString) =>  { new String(byteString.toArray) }
      )
    val header = Json.parse(message.frames(delimiterIndex + 2)).as[Header]
    val parentHeader = Json.parse(message.frames(delimiterIndex + 3)).validate[ParentHeader].fold[ParentHeader](
      // TODO: Investigate better solution than setting parentHeader to null for {}
      (invalid: Seq[(JsPath, Seq[ValidationError])]) => null, //HeaderBuilder.empty,
      (valid: ParentHeader) => valid
    )
    val metadata = Json.parse(message.frames(delimiterIndex + 4)).as[Metadata]

    new KernelMessage(ids,message.frame(delimiterIndex + 1),
      header, parentHeader, metadata, message.frame(delimiterIndex + 5))
  }

  implicit def KernelMessageToZMQMessage(kernelMessage : KernelMessage) : ZMQMessage = {
    val frames: scala.collection.mutable.ListBuffer[ByteString] = scala.collection.mutable.ListBuffer()
    kernelMessage.ids.map((id : String) => frames += id )
    frames += "<IDS|MSG>"
    frames += kernelMessage.signature
    frames += Json.toJson(kernelMessage.header).toString()
    frames += Json.toJson(kernelMessage.parentHeader).toString()
    frames += Json.toJson(kernelMessage.metadata).toString
    frames += kernelMessage.contentString
    ZMQMessage(frames  : _*)
  }

  def parseAndHandle[T, U](json: String, reads: Reads[T], handler: T => U) : U = {
    Json.parse(json).validate[T](reads).fold(
      (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
        logger.error(s"Could not parse JSON, ${json}")
        throw new Throwable(s"Could not parse JSON, ${json}")
      },
      (content: T) => handler(content)
    )
  }

}
