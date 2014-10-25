package com.ibm.spark.kernel.protocol

import akka.util.{ByteString, Timeout}
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.MIMEType.MIMEType
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.concurrent.duration._
import java.nio.charset.Charset

//
// NOTE: This is brought in to remove feature warnings regarding the use of
//       implicit conversions regarding the following:
//
//       1. ByteStringToString
//       2. ZMQMessageToKernelMessage
//
import scala.language.implicitConversions

package object v5 {
  // Provide a UUID type representing a string (there is no object)
  type UUID = String

  // Provide a ParentHeader type and object representing a Header
  type ParentHeader = Header
  val ParentHeader = Header

  // Provide a Metadata type and object representing a map
  type Metadata = Map[String, String]
  val Metadata = Map

  // Provide a Data type and object representing a map
  type Data = Map[MIMEType, String]
  val Data = Map

  // Provide a UserExpressions type and object representing a map
  type UserExpressions = Map[String, String]
  val UserExpressions = Map

  // Provide a Payloads type and object representing a list of maps
  type Payloads = List[Map[String, String]]
  val Payloads = List

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

  // TODO: Split this into client/server socket types and move them to their
  //       respective projects
  object SocketType extends Enumeration {
    type SocketType = Value

    // Server-specific actors
    val Shell       = Value("shell")
    val IOPub       = Value("io_pub")
    val StdIn       = Value("std_in")
    val Control     = Value("control")
    val Heartbeat   = Value("heartbeat")

    // Client-specific actors
    val ShellClient       = Value("shell_client")
    val IOPubClient       = Value("io_pub_client")
    val StdInClient       = Value("std_in_client")
    val ControlClient     = Value("control_client")
    val HeartbeatClient   = Value("heartbeat_client")
  }

  object MessageType extends Enumeration {
    type MessageType    = Value

    //  Shell Router/Dealer Messages
    val CompleteRequest = Value("complete_request")
    val CompleteReply   = Value("complete_reply")
    val ConnectRequest  = Value("connect_request")
    val ConnectReply    = Value("connect_reply")
    val ExecuteRequest  = Value("execute_request")
    val ExecuteReply    = Value("execute_reply")
    val HistoryRequest  = Value("history_request")
    val HistoryReply    = Value("history_reply")
    val InspectRequest  = Value("inspect_request")
    val InspectReply    = Value("inspect_reply")
    val KernelInfoRequest  = Value("kernel_info_request")
    val KernelInfoReply    = Value("kernel_info_reply")
    val ShutdownRequest = Value("shutdown_request")
    val ShutdownReply   = Value("shutdown_reply")

    //  Pub/Sub Messages
    val ClearOutput     = Value("clear_output")
    val DisplayData     = Value("display_data")
    val Error           = Value("error")
    val ExecuteInput    = Value("execute_input")
    val ExecuteResult   = Value("execute_result")
    val Status          = Value("status")
    val Stream          = Value("stream")
  }

  object HandlerType extends Enumeration {
    type HandlerType = Value

    val ExecuteRequestHandler = Value("execute_request_handler")
  }

  object SystemActorType extends Enumeration {
    type SystemActorType = Value

    val KernelMessageRelay  = Value("kernel_message_relay")
    val ExecuteRequestRelay = Value("execute_request_relay")
    val Interpreter         = Value("interpreter")
    val SignatureManager    = Value("signature_manager")
    val MagicManager        = Value("magic_manager")
    val StatusDispatch      = Value("status_dispatch")
  }

  object KernelStatusType extends Enumeration {
    type KernelStatusType = Value

    val Starting  = Value("starting")
    val Busy      = Value("busy")
    val Idle      = Value("idle")
  }

  object MIMEType extends Enumeration {
    type MIMEType = String

    val PlainText       = "text/plain"
    val ImagePng        = "image/png"
    val TextHtml        = "text/html"
    val ApplicationJson = "application/json"
  }
}