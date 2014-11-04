package com.ibm.spark.kernel.protocol.v5.stream

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest._
import play.api.libs.json._
import com.ibm.spark.kernel.protocol.v5.content.StreamContent

import scala.concurrent.duration._

class KernelMessageStreamSpec
  extends TestKit(ActorSystem("KernelMessageStreamActorSystem"))
  with FunSpecLike with Matchers with GivenWhenThen with BeforeAndAfter
  with MockitoSugar
{

  var actorLoader: ActorLoader = _
  var kernelMessageRelayProbe: TestProbe = _

  //
  // SHARED ELEMENTS BETWEEN TESTS
  //

  val executionCount = 3
  val skeletonKernelMessage = new KernelMessage(
    Nil, "", null, Header("", "", "", "", "5.0"), Metadata(), ""
  )

  before {
    // Create a mock ActorLoader for the KernelMessageStream we are testing
    actorLoader = mock[ActorLoader]

    // Create a probe for the relay and mock the ActorLoader to return the
    // associated ActorSelection
    kernelMessageRelayProbe = TestProbe()
    val kernelMessageRelaySelection: ActorSelection =
      system.actorSelection(kernelMessageRelayProbe.ref.path.toString)
    doReturn(kernelMessageRelaySelection)
      .when(actorLoader).load(SystemActorType.KernelMessageRelay)
  }

  describe("KernelMessageStream") {
    describe("#write(Int)") {
      it("should add a new byte to the internal list") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        )

        When("a byte is written to the stream")
        val expected = 'a'
        kernelMessageStream.write(expected)

        Then("it should be appended to the internal list")
        kernelMessageStream.flush()
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected.toString)
      }

      it("should call flush if the byte provided is a newline") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = spy(new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        ))

        When("a newline byte is written to the stream")
        val expected = '\n'
        kernelMessageStream.write(expected)

        Then("flush is called")
        verify(kernelMessageStream).flush()

        And("a message is sent")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected.toString)
      }

      it("should not call flush if the byte provided is not a newline") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = spy(new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        ))

        When("a non-newline byte is written to the stream")
        val expected = 'a'
        kernelMessageStream.write(expected)

        Then("flush is not called")
        verify(kernelMessageStream, never()).flush()

        And("no message is sent")
        kernelMessageRelayProbe.expectNoMsg(50.milliseconds)
      }
    }
    describe("#flush") {
      it("should clear the internal list of bytes") {

      }

      it("should set the ids of the kernel message") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the ids should be set to execute_result")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        message.ids should be (Seq(MessageType.Stream.toString))
      }

      it("should set the message type in the header of the kernel message to an execute_result") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the msg_type in the header should be execute_result")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        message.header.msg_type should be (MessageType.Stream.toString)
      }

      it("should set the content string of the kernel message") {
        Given("a kernel message stream with a skeleton kernel message")
        val kernelMessageStream = new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the content string should have text/plain set to the string")
        val message = kernelMessageRelayProbe
          .receiveOne(1.seconds).asInstanceOf[KernelMessage]
        val executeResult = Json.parse(message.contentString).as[StreamContent]
        executeResult.text should be (expected)
      }

      it("should make a copy of the kernel message skeleton") {
        // TODO: After refactoring, this no longer calls parentHeader.copy
        //       and instead invokes the HeaderBuilder object create method,
        //       which cannot be spied/mocked -- how do we test it?
        Given("a kernel message stream with a skeleton kernel message")
        val mockKernelMessage = mock[KernelMessage]
        doReturn(mockKernelMessage).when(mockKernelMessage).copy(
          any[Seq[String]], anyString(), any[Header], any[ParentHeader],
          any[Metadata], anyString()
        )

        val kernelMessageStream = new KernelMessageStream(
          actorLoader, mockKernelMessage
        )

        When("a string is written as the result and flushed")
        val expected = "some string"
        kernelMessageStream.write(expected.getBytes)
        kernelMessageStream.flush()

        Then("the resulting message should be a copy of the skeleton message")
        kernelMessageRelayProbe.expectMsgClass(classOf[KernelMessage])

        verify(mockKernelMessage).copy(
          any[Seq[String]], anyString(), any[Header], any[ParentHeader],
          any[Metadata], anyString()
        )
      }
    }
  }
}
