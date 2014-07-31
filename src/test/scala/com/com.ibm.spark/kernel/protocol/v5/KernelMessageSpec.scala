package com.ibm.kernel.protocol.v5

import akka.util.ByteString
import akka.zeromq.ZMQMessage
import org.scalatest.{FunSpec, Matchers}

class KernelMessageSpec extends FunSpec with Matchers {
  val header: Header = Header(
    "<UUID>", "<STRING>", "<UUID>", "<STRING>", "<FLOAT>"
  )
  val parentHeader : ParentHeader = ParentHeader(
    "<PARENT-UUID>", "<PARENT-STRING>", "<PARENT-UUID>", "<PARENT-STRING>", "<PARENT-FLOAT>"
  )
  val kernelMessage = KernelMessage(
    Seq("<STRING-1>","<STRING-2>"),
    "<SIGNATURE>", header, parentHeader, Map(), "<STRING>"
  )

  val zmqMessage = ZMQMessage(
    ByteString("<STRING-1>".getBytes()),
    ByteString("<STRING-2>".getBytes()),
    ByteString("<IDS|MSG>".getBytes()),
    ByteString("<SIGNATURE>".getBytes()),
    ByteString(
      """
      {
          "msg_id": "<UUID>",
          "username": "<STRING>",
          "session": "<UUID>",
          "msg_type": "<STRING>",
          "version": "<FLOAT>"
      }
      """.stripMargin.getBytes()),
    ByteString(
      """
      {
          "msg_id": "<PARENT-UUID>",
          "username": "<PARENT-STRING>",
          "session": "<PARENT-UUID>",
          "msg_type": "<PARENT-STRING>",
          "version": "<PARENT-FLOAT>"
      }
      """.stripMargin.getBytes()),
      ByteString("{}".getBytes()),
      ByteString("<STRING>".getBytes())
  )
  describe("KernelMessage") {
    describe("implicit conversions") {
      it("should implicitly convert from valid json to a displayData instance") {
        // This is the least safe way to convert as an error is thrown if it fails
        def printKernelMessage(message : KernelMessage ) = {
          message should be (kernelMessage)
        }
        printKernelMessage(zmqMessage)
      }
    }
  }
}
