/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package system

import akka.testkit.{TestKit, TestProbe}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.client.Utilities._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage, SocketType}
import org.scalatest.concurrent.Eventually
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import play.api.libs.json.Json
import test.utils.SparkClientDeployer

import scala.concurrent.duration._

/**
 * Used for system-wide (entire Spark Kernel) tests relating to Comm usage.
 *
 * @note Do not use non-generated target names or Comm ids! This is a shared
 *       kernel instance (to avoid death by slowness) and it is not guaranteed
 *       that previous tests will do proper cleanup!
 */
class ClientCommSpecForSystem
  extends TestKit(SparkClientDeployer.getClientActorSystem)
  with FunSpecLike with Matchers with BeforeAndAfterAll with Eventually
{
  private val MaxFishTime = 2.seconds
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(5, Milliseconds))
  )

  import test.utils.SparkClientDeployer._

  private def waitForExecuteReply(
    shell: TestProbe, headerId: v5.UUID, maxTime: Duration = MaxFishTime
  ): Unit =
    shell.fishForMessage(maxTime) {
      case KernelMessage(_, _, header, parentHeader, _, _)
        if header.msg_type == ExecuteReply.toTypeString &&
          parentHeader.msg_id == headerId=> true
      case _ => false
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

  describe("Comm for Client System") {
    describe("executing Comm API to open a new comm") {
      it("should correctly send comm_open to socket") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          client.comm.open(testTargetName)

          // Should discover an outgoing kernel message for comm_open
          shell.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommOpen.toTypeString => true
            case _ => false
          }
        }
      }
    }

    describe("executing Comm API to send a message") {
      it("should correctly send comm_msg to socket") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          val commWriter = client.comm.open(testTargetName)
          commWriter.writeMsg(v5.MsgData("key" -> "value"))
          // Should discover an outgoing kernel message for comm_msg
          shell.fishForMessage(MaxFishTime) {
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
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          val commWriter = client.comm.open(testTargetName)
          commWriter.close()

          // Should discover an outgoing kernel message for comm_close
          shell.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommClose.toTypeString => true
            case _ => false
          }
        }
      }
    }

    describe("receiving Comm API open from a kernel") {
      it("should respond comm_close if the target is not found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          // Send a comm_open (as if a kernel did it)
          val (_, message) = buildZMQCommOpen(testTargetName, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! message

          // Should discover an outgoing kernel message for comm_close
          shell.fishForMessage(MaxFishTime) {
            case KernelMessage(_, _, header, _, _, _)
              if header.msg_type == CommClose.toTypeString => true
            case _ => false
          }
        }
      }

      it("should not execute open callbacks if the target is not found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          var openExecuted = false
          client.comm.register(testTargetName).addOpenHandler {
            (_, _, _, _) => openExecuted = true
          }

          // Send a comm_open (as if a kernel did it)
          val (_, message) = buildZMQCommOpen(testTargetName, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! message

          eventually { openExecuted should be (true) }
        }
      }

      it("should execute open callbacks if the target is found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString

          var openExecuted = false
          client.comm.register(testTargetName).addOpenHandler {
            (_, _, _, _) => openExecuted = true
          }

          // Send a comm_open (as if a kernel did it)
          val (_, message) = buildZMQCommOpen(testTargetName, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! message

          eventually { openExecuted should be (true) }
        }
      }
    }

    describe("receiving Comm API message from a client") {
      it("should not execute message callbacks if the Comm id is not found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString

          var msgExecuted = false

          client.comm.register(testTargetName)
            .addCloseHandler { (_, _, _) => msgExecuted = true }

          // Send a comm_close (as if a kernel did it)
          val (_, msgMessage) = buildZMQCommMsg(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! msgMessage

          intercept[TestFailedDueToTimeoutException] {
            eventually { msgExecuted should be (true) }
          }
        }
      }

      it("should execute message callbacks if the Comm id is found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString

          var openExecuted = false
          var msgExecuted = false

          client.comm.register(testTargetName)
            .addOpenHandler { (_, _, _, _) => openExecuted = true }
            .addMsgHandler { (_, _, _) => msgExecuted = true }

          // Send a comm_open (as if a kernel did it)
          val (_, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.IOPubClient) ! openMessage

          eventually { openExecuted should be (true) }

          // Send a comm_msg (as if a kernel did it)
          val (_, msgMessage) = buildZMQCommMsg(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! msgMessage

          eventually { msgExecuted should be (true) }
        }
      }
    }

    describe("receiving Comm API close from a client") {
      it("should not execute close callbacks if the Comm id is not found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString

          var closeExecuted = false

          client.comm.register(testTargetName)
            .addCloseHandler { (_, _, _) => closeExecuted = true }

          // Send a comm_close (as if a kernel did it)
          val (_, closeMessage) = buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! closeMessage

          intercept[TestFailedDueToTimeoutException] {
            eventually { closeExecuted should be (true) }
          }
        }
      }

      it("should execute close callbacks if the Comm id is found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString

          var openExecuted = false
          var closeExecuted = false

          client.comm.register(testTargetName)
            .addOpenHandler { (_, _, _, _) => openExecuted = true }
            .addCloseHandler { (_, _, _) => closeExecuted = true }

          // Send a comm_open (as if a kernel did it)
          val (_, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.IOPubClient) ! openMessage

          eventually { openExecuted should be (true) }

          // Send a comm_close (as if a kernel did it)
          val (_, closeMessage) = buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! closeMessage

          eventually { closeExecuted should be (true) }
        }
      }

      it("should unlink the Comm id from the target if the Comm id is found") {
        withSparkClient { (client, actorLoader, heartbeat, stdin, shell, ioPub) =>
          val testTargetName = java.util.UUID.randomUUID().toString
          val testCommId = java.util.UUID.randomUUID().toString

          var openExecuted = false
          var closeExecuted = false

          client.comm.register(testTargetName)
            .addOpenHandler { (_, _, _, _) => openExecuted = true }
            .addCloseHandler { (_, _, _) => closeExecuted = true }

          // Send a comm_open (as if a kernel did it)
          val (_, openMessage) =
            buildZMQCommOpen(testTargetName, v5.MsgData.Empty, testCommId)
          actorLoader.load(SocketType.IOPubClient) ! openMessage

          eventually { openExecuted should be (true) }

          // Send a comm_close (as if a kernel did it)
          val (_, closeMessage1) = buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! closeMessage1

          eventually { closeExecuted should be (true) }

          // Reset close event flag
          closeExecuted = false

          // Send a comm_close (again)
          val (_, closeMessage2) = buildZMQCommClose(testCommId, v5.MsgData.Empty)
          actorLoader.load(SocketType.IOPubClient) ! closeMessage2

          intercept[TestFailedDueToTimeoutException] {
            eventually { closeExecuted should be (true) }
          }
        }
      }
    }
  }
}
