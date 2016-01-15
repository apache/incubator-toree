package org.apache.toree.kernel.api

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.KernelMessage
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, BeforeAndAfter, Matchers, FunSpec}
import play.api.libs.json.Json

import scala.concurrent.duration._

import org.mockito.Mockito._

class StreamMethodsSpec extends TestKit(
  ActorSystem("StreamMethodsSpec")
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val MaxDuration = 300.milliseconds

  private var kernelMessageRelayProbe: TestProbe = _
  private var mockParentHeader: v5.ParentHeader = _
  private var mockActorLoader: v5.kernel.ActorLoader = _
  private var mockKernelMessage: v5.KernelMessage = _
  private var streamMethods: StreamMethods = _

  before {
    kernelMessageRelayProbe = TestProbe()

    mockParentHeader = mock[v5.ParentHeader]

    mockActorLoader = mock[v5.kernel.ActorLoader]
    doReturn(system.actorSelection(kernelMessageRelayProbe.ref.path))
      .when(mockActorLoader).load(v5.SystemActorType.KernelMessageRelay)

    mockKernelMessage = mock[v5.KernelMessage]
    doReturn(mockParentHeader).when(mockKernelMessage).header

    streamMethods = new StreamMethods(mockActorLoader, mockKernelMessage)
  }

  describe("StreamMethods") {
    describe("#()") {
      it("should put the header of the given message as the parent header") {
        val expected = mockKernelMessage.header
        val actual = streamMethods.kmBuilder.build.parentHeader

        actual should be (expected)
      }
    }

    describe("#sendAll") {
      it("should send a message containing all of the given text") {
        val expected = "some text"

        streamMethods.sendAll(expected)

        val outgoingMessage = kernelMessageRelayProbe.receiveOne(MaxDuration)
        val kernelMessage = outgoingMessage.asInstanceOf[KernelMessage]

        val actual = Json.parse(kernelMessage.contentString)
          .as[v5.content.StreamContent].text

        actual should be (expected)
      }
    }
  }

}
