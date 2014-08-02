package com.ibm.spark.kernel.protocol

import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5._

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

}
