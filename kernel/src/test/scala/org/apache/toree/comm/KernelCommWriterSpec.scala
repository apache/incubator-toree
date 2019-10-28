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
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import play.api.libs.json.Json
import test.utils.MaxAkkaTestTimeout
import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.mockito.Mockito._
import org.mockito.Matchers._

object KernelCommWriterSpec {
  val config ="""
    akka {
      loglevel = "WARNING"
    }"""
}

class KernelCommWriterSpec extends TestKit(
  ActorSystem("KernelCommWriterSpec",
    ConfigFactory.parseString(KernelCommWriterSpec.config),
    org.apache.toree.Main.getClass.getClassLoader
  )
) with FunSpecLike with Matchers with BeforeAndAfter with MockitoSugar
{

  private val commId = UUID.randomUUID().toString
  private var kernelCommWriter: KernelCommWriter = _
  private var kernelMessageBuilder: KMBuilder = _

  private var actorLoader: ActorLoader = _
  private var kernelMessageRelayProbe: TestProbe = _

  /**
   * Retrieves the next message available.
   *
   * @return The KernelMessage instance (or an error if timed out)
   */
  private def getNextMessage =
    kernelMessageRelayProbe.receiveOne(MaxAkkaTestTimeout)
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
    kernelMessageRelayProbe = TestProbe()
    val kernelMessageRelaySelection: ActorSelection =
      system.actorSelection(kernelMessageRelayProbe.ref.path.toString)
    doReturn(kernelMessageRelaySelection)
      .when(actorLoader).load(SystemActorType.KernelMessageRelay)

    // Create a new writer to use for testing
    kernelCommWriter = new KernelCommWriter(actorLoader, kernelMessageBuilder, commId)
  }

  describe("KernelCommWriter") {
    describe("#writeOpen") {
      it("should send a comm_open message to the relay") {
        kernelCommWriter.writeOpen(anyString())

        getNextMessageType should be (CommOpen.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        kernelCommWriter.writeOpen(anyString())

        val actual = getNextMessageContents[CommOpen].comm_id

        actual should be (expected)
      }

      it("should include the target name in the message") {
        val expected = "<TARGET_NAME>"
        kernelCommWriter.writeOpen(expected)

        val actual = getNextMessageContents[CommOpen].target_name

        actual should be (expected)
      }

      it("should provide empty data in the message if no data is provided") {
        val expected = MsgData.Empty
        kernelCommWriter.writeOpen(anyString())

        val actual = getNextMessageContents[CommOpen].data

        actual should be (expected)
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        kernelCommWriter.writeOpen(anyString(), expected)

        val actual = getNextMessageContents[CommOpen].data

        actual should be (expected)
      }
    }

    describe("#writeMsg") {
      it("should send a comm_msg message to the relay") {
        kernelCommWriter.writeMsg(MsgData.Empty)

        getNextMessageType should be (CommMsg.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        kernelCommWriter.writeMsg(MsgData.Empty)

        val actual = getNextMessageContents[CommMsg].comm_id

        actual should be (expected)
      }

      it("should fail a require if the data is null") {
        intercept[IllegalArgumentException] {
          kernelCommWriter.writeMsg(null)
        }
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        kernelCommWriter.writeMsg(expected)

        val actual = getNextMessageContents[CommMsg].data

        actual should be (expected)
      }
    }

    describe("#writeClose") {
      it("should send a comm_close message to the relay") {
        kernelCommWriter.writeClose()

        getNextMessageType should be (CommClose.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        kernelCommWriter.writeClose()

        val actual = getNextMessageContents[CommClose].comm_id

        actual should be (expected)
      }

      it("should provide empty data in the message if no data is provided") {
        val expected = MsgData.Empty
        kernelCommWriter.writeClose()

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }

      it("should include the data in the message") {
        val expected = MsgData("some key" -> "some value")
        kernelCommWriter.writeClose(expected)

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }
    }

    describe("#write") {
      it("should send a comm_msg message to the relay") {
        kernelCommWriter.write(Array('a'), 0, 1)

        getNextMessageType should be (CommMsg.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        kernelCommWriter.write(Array('a'), 0, 1)

        val actual = getNextMessageContents[CommMsg].comm_id

        actual should be (expected)
      }

      it("should package the string as part of the data with a 'message' key") {
        val expected = MsgData("message" -> "a")
        kernelCommWriter.write(Array('a'), 0, 1)

        val actual = getNextMessageContents[CommMsg].data

        actual should be (expected)
      }
    }

    describe("#flush") {
      it("should do nothing") {
        // TODO: Is this test necessary? It does nothing.
        kernelCommWriter.flush()
      }
    }

    describe("#close") {
      it("should send a comm_close message to the relay") {
        kernelCommWriter.close()

        getNextMessageType should be (CommClose.toTypeString)
      }

      it("should include the comm_id in the message") {
        val expected = commId
        kernelCommWriter.close()

        val actual = getNextMessageContents[CommClose].comm_id

        actual should be (expected)
      }

      it("should provide empty data in the message") {
        val expected = MsgData.Empty
        kernelCommWriter.close()

        val actual = getNextMessageContents[CommClose].data

        actual should be (expected)
      }
    }
  }
}
