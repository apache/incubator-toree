/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package system

import akka.testkit.{TestProbe}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import Utilities._
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.{KernelMessage, SocketType, KMBuilder}
import org.apache.toree.kernel.protocol.v5.content._
import org.scalatest._
import play.api.libs.json.Json
import scala.concurrent.duration._
import test.utils.{NoArgSparkKernelTestKit}


/**
 * Used for system-wide (entire Spark Kernel) tests relating to Comm usage.
 *
 * @note Do not use non-generated target names or Comm ids! This is a shared
 *       kernel instance (to avoid death by slowness) and it is not guaranteed
 *       that previous tests will do proper cleanup!
 */
@DoNotDiscover
class KernelCommSpecForSystem
  extends NoArgSparkKernelTestKit
  with FunSpecLike with Matchers
{
  private val MaxFishTime = 5.seconds

  import org.apache.toree.boot.layer.SparkKernelDeployer._

  private def waitForExecuteReply(
    shell: TestProbe, headerId: v5.UUID, maxTime: Duration = MaxFishTime
  ): Unit =
    shell.fishForMessage(maxTime) {
      case KernelMessage(_, _, header, parentHeader, _, _)
        if header.msg_type == ExecuteReply.toTypeString &&
          parentHeader.msg_id == headerId=> true
      case _ => false
    }

  private def waitForBusyStatus(
    ioPub: TestProbe, headerId: v5.UUID, maxTime: Duration = MaxFishTime
  ): Unit =
    ioPub.fishForMessage(maxTime) {
      case KernelMessage(_, _, header, parentHeader, _, contentString)
        if header.msg_type == KernelStatus.toTypeString &&
          parentHeader.msg_id == headerId =>
        Json.parse(contentString).as[KernelStatus].execution_state == "busy"
      case _ => false
    }

  private def waitForIdleStatus(
    ioPub: TestProbe, headerId: v5.UUID, maxTime: Duration = MaxFishTime
  ): Unit =
    ioPub.fishForMessage(maxTime) {
      case KernelMessage(_, _, header, parentHeader, _, contentString)
        if header.msg_type == KernelStatus.toTypeString &&
          parentHeader.msg_id == headerId =>
        Json.parse(contentString).as[KernelStatus].execution_state == "idle"
      case _ => false
    }

  private def setupCallback(
    actorLoader: ActorLoader, shell: TestProbe, callbackFunc: String,
    targetName: String, expectedMessage: String
  ) = {
    // If open callback, there are four arguments
    // If msg/close callback, there are three arguments
    val callbackArgs =
      if (callbackFunc.toLowerCase.trim == "addopenhandler") """(_,_,_,_)"""
      else """(_,_,_)"""

    // Set up a callback (using stream messages as indicator)
    val codeRequest = s"""
      |kernel.comm.register("$targetName").$callbackFunc {
      |    $callbackArgs => kernel.out.println("$expectedMessage")
      |}
    """.stripMargin.trim
    val (messageId, message) = buildZMQExecuteRequest(codeRequest)
    actorLoader.load(SocketType.Shell) ! message

    waitForExecuteReply(shell, messageId)
  }

  private def testForCallback(
    ioPub: TestProbe, expectedMessage: String, maxTime: Duration = MaxFishTime
  ) = {
    // Should discover an outgoing kernel message for stream
    ioPub.fishForMessage(maxTime) {
      case KernelMessage(_, _, header, _, _, contentString)
        if header.msg_type == StreamContent.toTypeString =>
        val callbackMessage =
          Json.parse(contentString).as[StreamContent].text.trim
        callbackMessage == expectedMessage
      case _ => false
    }
  }

  private def buildZMQExecuteRequest(codeRequest: String): (v5.UUID, ZMQMessage) = {
    val kernelMessage = KMBuilder()
      .withHeader(ExecuteRequest.toTypeString)
      .withContentString(ExecuteRequest(
      code = codeRequest,
      silent = false,
      store_history = false,
      user_expressions = v5.UserExpressions(),
      allow_stdin = true
    )).build

    (kernelMessage.header.msg_id, kernelMessage)
  }

  // Not using TestCommId to avoid problems if previous comm using that
  // id in tests was not properly closed
  private def buildZMQCommOpen(
    targetName: String, data: v5.MsgData,
    commId: v5.UUID = java.util.UUID.randomUUID().toString
  ): (v5.UUID, ZMQMessage) = {
    val kernelMessage = KMBuilder()
      .withHeader(CommOpen.toTypeString)
      .withContentString(CommOpen(
      comm_id = commId,
      target_name = targetName,
      data = data
    )).build

    (kernelMessage.header.msg_id, kernelMessage)
  }

  // Not using TestCommId to avoid problems if previous comm using that
  // id in tests was not properly closed
  private def buildZMQCommMsg(commId: v5.UUID, data: v5.MsgData): (v5.UUID, ZMQMessage) = {
    val kernelMessage = KMBuilder()
      .withHeader(CommMsg.toTypeString)
      .withContentString(CommMsg(
      comm_id = commId,
      data = data
    )).build

    (kernelMessage.header.msg_id, kernelMessage)
  }

  // Not using TestCommId to avoid problems if previous comm using that
  // id in tests was not properly closed
  private def buildZMQCommClose(commId: v5.UUID, data: v5.MsgData): (v5.UUID, ZMQMessage) = {
    val kernelMessage = KMBuilder()
      .withHeader(CommClose.toTypeString)
      .withContentString(CommClose(
      comm_id = commId,
      data = data
    )).build

    (kernelMessage.header.msg_id, kernelMessage)
  }

  describe("Comm for Kernel System") {
    describe("executing Comm API to open a new comm") {
      it("should correctly send comm_open to socket") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val codeRequest =
            s"""
              |kernel.comm.open("$testTargetName")
            """.stripMargin.trim

          // Send our code to trigger the Comm API
          val (_, message) = buildZMQExecuteRequest(codeRequest)
          actorLoader.load(SocketType.Shell) ! message

          // Should discover an outgoing kernel message for comm_open
          ioPub.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommOpen.toTypeString => true
            case _ => false
          }
        }
      }
    }

    describe("executing Comm API to send a message") {
      it("should correctly send comm_msg to socket") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val codeRequest =
            s"""
              |val commWriter = kernel.comm.open("$testTargetName")
              |commWriter.writeMsg(
              |   org.apache.toree.kernel.protocol.v5.MsgData("key" -> "value")
              |)
            """.stripMargin.trim

          // Send our code to trigger the Comm API
          val (_, message) = buildZMQExecuteRequest(codeRequest)
          actorLoader.load(SocketType.Shell) ! message

          // Should discover an outgoing kernel message for comm_msg
          ioPub.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, contentString)
              if header.msg_type == CommMsg.toTypeString =>
              (Json.parse(contentString).as[CommMsg].data \ "key").as[String] == "value"
            case _ => false
          }
        }
      }
    }

    describe("executing Comm API to close an existing comm") {
      it("should correctly send comm_close to socket") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val codeRequest =
            s"""
              |val commWriter = kernel.comm.open("$testTargetName")
              |commWriter.close()
            """.stripMargin.trim

          // Send our code to trigger the Comm API
          val (_, message) = buildZMQExecuteRequest(codeRequest)
          actorLoader.load(SocketType.Shell) ! message

          // Should discover an outgoing kernel message for comm_close
          ioPub.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommClose.toTypeString => true
            case _ => false
          }
        }
      }
    }

    describe("receiving Comm API open from a client") {
      it("should respond comm_close if the target is not found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          // Send a comm_open (as if a client did it)
          val (_, message) = buildZMQCommOpen(testTargetName, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! message

          // Should discover an outgoing kernel message for comm_close
          ioPub.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommClose.toTypeString => true
            case _ => false
          }
        }
      }

        it("should not execute open callbacks if the target is not found") {
          withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
            val testTargetName = java.util.UUID.randomUUID().toString
            val expectedMessage = "callback" + testTargetName

            setupCallback(actorLoader, shell, "addOpenHandler",
              testTargetName, expectedMessage)

            // Send a comm_open (as if a client did it) with wrong target
            val (_, message) =
              buildZMQCommOpen(testTargetName + "wrong", v5.MsgData.Empty)
            actorLoader.load(SocketType.Shell) ! message

            // Throws an assertion error when a timeout occurs
            intercept[AssertionError] {
              testForCallback(ioPub, expectedMessage, 3.seconds)
            }
          }
        }

      it("should execute open callbacks if the target is found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addOpenHandler",
            testTargetName, expectedMessage)

          // Send a comm_open (as if a client did it)
          val (_, message) = buildZMQCommOpen(testTargetName, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! message

          testForCallback(ioPub, expectedMessage)
        }
      }

      // Tested through other tests
      /*ignore("should link the target and Comm id if the target is found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
        }
      }*/
    }

    describe("receiving Comm API message from a client") {
      it("should not execute message callbacks if the Comm id is not found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addMsgHandler",
            testTargetName, expectedMessage)

          // Set up the Comm id prior so we have a proper link
          val (openId, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.Shell) ! openMessage

          waitForIdleStatus(ioPub, openId)

          // Send a comm_msg (as if a client did it) with wrong id
          val (msgId, msgMessage) =
            buildZMQCommMsg(testCommId + "wrong", v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! msgMessage

          // Throws an assertion error when a timeout occurs
          intercept[AssertionError] {
            testForCallback(ioPub, expectedMessage, 3.seconds)
          }
        }
      }

      it("should execute message callbacks if the Comm id is found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addMsgHandler",
            testTargetName, expectedMessage)

          // Set up the Comm id prior so we have a proper link
          val (openId, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.Shell) ! openMessage

          waitForIdleStatus(ioPub, openId)

          // Send a comm_msg (as if a client did it)
          val (msgId, msgMessage) =
            buildZMQCommMsg(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! msgMessage

          testForCallback(ioPub, expectedMessage)
        }
      }
    }

    describe("receiving Comm API close from a client") {
      it("should not execute close callbacks if the Comm id is not found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addCloseHandler",
            testTargetName, expectedMessage)

          // Set up the Comm id prior so we have a proper link
          val (openId, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.Shell) ! openMessage

          waitForIdleStatus(ioPub, openId)

          // Send a comm_close (as if a client did it) with wrong id
          val (closeId, closeMessage) =
            buildZMQCommClose(testCommId + "wrong", v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! closeMessage

          // Throws an assertion error when a timeout occurs
          intercept[AssertionError] {
            testForCallback(ioPub, expectedMessage, 3.seconds)
          }
        }
      }

      it("should execute close callbacks if the Comm id is found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addCloseHandler",
            testTargetName, expectedMessage)

          // Set up the Comm id prior so we have a proper link
          val (openId, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.Shell) ! openMessage

          waitForIdleStatus(ioPub, openId)

          // Send a comm_close (as if a client did it)
          val (closeId, closeMessage) =
            buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! closeMessage

          testForCallback(ioPub, expectedMessage)
        }
      }

      it("should unlink the Comm id from the target if the Comm id is found") {
        withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString
          val expectedMessage = "callback" + testTargetName

          setupCallback(actorLoader, shell, "addCloseHandler",
            testTargetName, expectedMessage)

          // Set up the Comm id prior so we have a proper link
          val (openId, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.Shell) ! openMessage

          waitForIdleStatus(ioPub, openId)

          // Send a comm_close (as if a client did it)
          val (closeId, closeMessage) =
            buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! closeMessage

          testForCallback(ioPub, expectedMessage)

          // Send a comm_close (again) with the expectation that nothing happens
          val (closeId2, closeMessage2) =
            buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.Shell) ! closeMessage2

          intercept[AssertionError] {
            testForCallback(ioPub, expectedMessage)
          }
        }
      }
    }
  }

}
