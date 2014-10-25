package com.ibm.spark.kernel.protocol

import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{CompleteRequest, ExecuteRequest}
import play.api.libs.json.Json

package object v5Test {
  //  The header for the message
  val MockHeader : Header = Header("<UUID>","<USER>","<SESSION>",
    MessageType.ClearOutput.toString, "<VERSION>")
  //  The parent header for the message
  val MockParenHeader: Header = Header("<PARENT-UUID>","<PARENT-USER>","<PARENT-SESSION>",
    MessageType.ClearOutput.toString, "<PARENT-VERSION>")
  //  The actual kernel message
  val MockKernelMessage : KernelMessage = KernelMessage(Seq("<ID>"), "<SIGNATURE>", MockHeader,
    MockParenHeader, Metadata(), "<CONTENT>")
  //  Use the implicit to convert the KernelMessage to ZMQMessage
  val MockZMQMessage : ZMQMessage = MockKernelMessage

  val MockExecuteRequest: ExecuteRequest =
    ExecuteRequest("spark code", false, true, Map(), false)
  val MockExecuteRequestKernelMessage = MockKernelMessage.copy(
    contentString =  Json.toJson(MockExecuteRequest).toString
  )
  val MockKernelMessageWithBadExecuteRequest = new KernelMessage(
    Seq[String](), "test message", MockHeader, MockParenHeader, Map[String, String](),
    """
        {"code" : 124 }
    """
  )
  val MockCompleteRequest: CompleteRequest = CompleteRequest("", 0)
  val MockCompleteRequestKernelMessage: KernelMessage = MockKernelMessage.copy(contentString = Json.toJson(MockCompleteRequest).toString)
  val MockKernelMessageWithBadJSON: KernelMessage = MockKernelMessage.copy(contentString = "inval1d")
}
