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

package org.apache.toree.comm

import java.util.UUID

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.content._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

object ClientCommWriterSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class ClientCommWriterSpec extends TestKit(
  ActorSystem("ClientCommWriterSpec",
    ConfigFactory.parseString(ClientCommWriterSpec.config))
) with FunSpecLike with Matchers with BeforeAndAfter with MockitoSugar
{

  private val commId = UUID.randomUUID().toString
  private var clientCommWriter: ClientCommWriter = _
  private var kernelMessageBuilder: KMBuilder = _

  private var actorLoader: ActorLoader = _
  private var shellSocketProbe: TestProbe = _

  /**
   * Retrieves the next message available.
   *
   * @return The KernelMessage instance (or an error if timed out)
   */
  private def getNextMessage =
    shellSocketProbe.receiveOne(200.milliseconds)
      .asInstanceOf[KernelMessage]

  /**
   * Retrieves the next message available and returns its type.
   *
   * @return The type of the message (pulled from message header)
   */
  private def getNextMessageType = getNextMessage.header.msg_type

  /**
   * Retrieves the next message available and parses the content string.
   *
   * @tparam T The type to coerce the content string into
   *
   * @return The resulting KernelMessageContent instance
   */
  private def getNextMessageContents[T <: KernelMessageContent]
    (implicit fjs: play.api.libs.json.Reads[T], mf: Manifest[T]) =
  {
    val receivedMessage = getNextMessage

    Json.parse(receivedMessage.contentString).as[T]
  }

  before {
    kernelMessageBuilder = spy(KMBuilder())

    // Construct path for kernel message relay
    actorLoader = mock[ActorLoader]
    shellSocketProbe = TestProbe()
    val shellSocketSelection: ActorSelection =
      system.actorSelection(shellSocketProbe.ref.path.toString)
    doReturn(shellSocketSelection)
      .when(actorLoader).load(SocketType.ShellClient)

    // Create a new writer to use for testing
    clientCommWriter =
      new ClientCommWriter(actorLoader, kernelMessageBuilder, commId)
  }

  describe("ClientCommWriter") {
    describe("#writeOpen") {
      it("should send a comm_open message to the relay") {
        clientCommWriter.writeOpen(anyString())

        getNextMessageType should be (CommOpen.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        clientCommWriter.writeOpen(anyString())

        val actual = getNextMessageContents[CommOpen].comm_id

        actual should be (expected)
      }

      it("should include the target name in the message") {
        val expected = "<TARGET_NAME>"
        clientCommWriter.writeOpen(expected)

        val actual = getNextMessageContents[CommOpen].target_name

        actual should be (expected)
      }

      it("should provide empty data in the message if no data is provided") {
        val expected = MsgData.Empty
        clientCommWriter.writeOpen(anyString())

        val actual = getNextMessageContents[CommOpen].data

        actual should be (expected)
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        clientCommWriter.writeOpen(anyString(), expected)

        val actual = getNextMessageContents[CommOpen].data

        actual should be (expected)
      }
    }

    describe("#writeMsg") {
      it("should send a comm_msg message to the relay") {
        clientCommWriter.writeMsg(MsgData.Empty)

        getNextMessageType should be (CommMsg.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        clientCommWriter.writeMsg(MsgData.Empty)

        val actual = getNextMessageContents[CommMsg].comm_id

        actual should be (expected)
      }

      it("should fail a require if the data is null") {
        intercept[IllegalArgumentException] {
          clientCommWriter.writeMsg(null)
        }
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        clientCommWriter.writeMsg(expected)

        val actual = getNextMessageContents[CommMsg].data

        actual should be (expected)
      }
    }

    describe("#writeClose") {
      it("should send a comm_close message to the relay") {
        clientCommWriter.writeClose()

        getNextMessageType should be (CommClose.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        clientCommWriter.writeClose()

        val actual = getNextMessageContents[CommClose].comm_id

        actual should be (expected)
      }

      it("should provide empty data in the message if no data is provided") {
        val expected = MsgData.Empty
        clientCommWriter.writeClose()

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        clientCommWriter.writeClose(expected)

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }
    }

    describe("#write") {
      it("should send a comm_msg message to the relay") {
        clientCommWriter.write(Array('a'), 0, 1)

        getNextMessageType should be (CommMsg.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        clientCommWriter.write(Array('a'), 0, 1)

        val actual = getNextMessageContents[CommMsg].comm_id

        actual should be (expected)
      }

      it("should package the string as part of the data with a 'message' key") {
        val expected = MsgData("message" -> "a")
        clientCommWriter.write(Array('a'), 0, 1)

        val actual = getNextMessageContents[CommMsg].data

        actual should be (expected)
      }
    }

    describe("#flush") {
      it("should do nothing") {
        // TODO: Is this test necessary? It does nothing.
        clientCommWriter.flush()
      }
    }

    describe("#close") {
      it("should send a comm_close message to the relay") {
        clientCommWriter.close()

        getNextMessageType should be (CommClose.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        clientCommWriter.close()

        val actual = getNextMessageContents[CommClose].comm_id

        actual should be (expected)
      }

      it("should provide empty data in the message") {
        val expected = MsgData.Empty
        clientCommWriter.close()

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }
    }
  }
}
