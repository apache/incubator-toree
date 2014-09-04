package integration.stream

import java.io.PrintStream

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.StreamContent
import com.ibm.spark.kernel.protocol.v5.stream.KernelMessageStream
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json._

import scala.concurrent.duration._

class KernelMessageStreamThroughPrintStreamSpecForIntegration
  extends TestKit(ActorSystem("KernelMessageStreamThroughPrintStreamActorSystem"))
  with FunSpecLike with Matchers with GivenWhenThen with BeforeAndAfter
  with MockitoSugar
{

  var actorLoader: ActorLoader = _
  var kernelMessageRelayProbe: TestProbe = _

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

  describe("KernelMessageStream through PrintStream") {
    describe("#println") {
      it("should flush the stream on each call") {
        Given("a kernel message stream with a skeleton kernel message")
        val executionCount = 3
        val skeletonKernelMessage = new KernelMessage(
          Nil, "", null, Header("", "", "", "", "5.0"), Metadata(), ""
        )
        val kernelMessageStream = spy(new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        ))
        val printStream = new PrintStream(kernelMessageStream)

        When("println is called")
        val expected = "some string\nadfnasdfklj\nasdf"
        printStream.println(expected)

        Then("flush should have been called three times")
        verify(kernelMessageStream, times(3)).flush()

        And("three messages should have been sent making up the content")
        val messages = kernelMessageRelayProbe
          .receiveN(3).map(_.asInstanceOf[KernelMessage])
        messages.foldLeft("")( (result: String, message: KernelMessage) => {
          val executeResult = Json.parse(message.contentString).as[StreamContent]
          result + executeResult.data
        }) should be (expected + "\n")
      }
    }
    describe("#print") {
      it("should flush the stream on receiving a newline") {
        Given("a kernel message stream with a skeleton kernel message")
        val executionCount = 3
        val skeletonKernelMessage = new KernelMessage(
          Nil, "", null, Header("", "", "", "", "5.0"), Metadata(), ""
        )
        val kernelMessageStream = spy(new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        ))
        val printStream = new PrintStream(kernelMessageStream)

        When("print is called with a newline")
        val expected = "some string"
        printStream.print(expected + "\n")

        Then("flush should have been called once")
        verify(kernelMessageStream).flush()

        And("the message should be sent")
        kernelMessageRelayProbe.expectMsgClass(classOf[KernelMessage])
      }

      it("should not flush the stream when no newline is received") {
        Given("a kernel message stream with a skeleton kernel message")
        val executionCount = 3
        val skeletonKernelMessage = new KernelMessage(
          Nil, "", null, Header("", "", "", "", "5.0"), Metadata(), ""
        )
        val kernelMessageStream = spy(new KernelMessageStream(
          actorLoader, skeletonKernelMessage
        ))
        val printStream = new PrintStream(kernelMessageStream)

        When("print is called without a newline")
        val expected = "some string"
        printStream.print(expected)

        Then("flush should not have been called")
        verify(kernelMessageStream, never()).flush()

        And("no message should have been sent")
        kernelMessageRelayProbe.expectNoMsg(50.milliseconds)
      }
    }
  }
}
