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

import akka.testkit.TestProbe
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5.kernel.{ActorLoader, Utilities}
import Utilities._
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.{KernelMessage, SocketType, KMBuilder}
import org.apache.toree.kernel.protocol.v5.content._
import org.scalatest._
import play.api.libs.json.Json
import scala.concurrent.duration._
import test.utils.NoArgSparkKernelTestKit
import org.apache.toree.boot.layer.SparkKernelDeployer


/**
 * Used for system-wide (entire Spark Kernel) tests relating to Comm usage.
 *
 * @note Do not use non-generated target names or Comm ids! This is a shared
 *       kernel instance (to avoid death by slowness) and it is not guaranteed
 *       that previous tests will do proper cleanup!
 */
@DoNotDiscover
class TruncationTests
  extends NoArgSparkKernelTestKit
  with FunSpecLike with Matchers
{
  private val MaxFishTime =   30.seconds

  import SparkKernelDeployer._

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


  private def executeCode(codeRequest : String, actorLoader : ActorLoader, ioPub: TestProbe ) : String = {
    val testTargetName = java.util.UUID.randomUUID().toString


    // Send our code to trigger the Comm API
    val (messageid, message) = buildZMQExecuteRequest(codeRequest)
    actorLoader.load(SocketType.Shell) ! message

    // Should discover an outgoing kernel message for comm_open
    val res= ioPub.fishForMessage(MaxFishTime) {
      case KernelMessage(_, _, header, parentHeader, _, _)
        if header.msg_type == ExecuteResult.toTypeString => true
      case _ => false
    }
    val kernelMessage=res.asInstanceOf[KernelMessage]
    val map=Json.parse(kernelMessage.contentString).as[ExecuteResult].data.asInstanceOf[Map[String,String]]
    val str= map.getOrElse("text/plain","NOT THERE")

    str
  }




  describe("Test Truncation") {
    it("should show or not show types based on %showtypes") {
      withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>

        executeCode("1",actorLoader,ioPub) should be ("1")

        executeCode(
          """%showtypes on
            |1
          """.stripMargin,actorLoader,ioPub) should be ("Int = 1")

        executeCode(
          """%showtypes off
            |1
          """.stripMargin,actorLoader,ioPub) should be ("1")
      }
    }
    it("should truncate or not turncate based on %truncate") {
      withNoArgSparkKernel { (actorLoader, heartbeat, shell, ioPub) =>

        executeCode("for ( a <-  1 to 300 ) yield a",actorLoader,ioPub) should endWith("...")

        executeCode(
          """%Truncation off
            |for ( a <-  1 to 300 ) yield a
          """.stripMargin,actorLoader,ioPub) should endWith("300)")



        executeCode(
          """%Truncation on
            |for ( a <-  1 to 300 ) yield a
          """.stripMargin,actorLoader,ioPub) should endWith("...")



      }
    }
    }


  }
