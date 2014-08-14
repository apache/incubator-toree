package com.ibm.spark.client

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.ibm.spark.client.exception.ShellException
import com.ibm.spark.kernel.protocol.v5.ActorLoader
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

import scala.util.{Failure, Success}

/**
 * Created by Chris on 8/12/14.
 */
class SparkKernelClientSpec extends TestKit(ActorSystem("RelayActorSystem"))
  with Matchers with MockitoSugar with FunSpecLike {

  // Receives messages from callbacks; used for assertions
  val actorLoader = mock[ActorLoader]
  val client = new SparkKernelClient(actorLoader)

  describe("SparkKernelClient") {
    val replyMessage = new ExecuteReply("", 1, None, None, None, None, None)

    var capturedReply: Option[ExecuteReply] = None
    val replyCallback = { e: ExecuteReply => capturedReply = Option(e) }

    var capturedError: Option[ShellException] = None
    val errorCallback = { e: ShellException => capturedError = Option(e) }

    describe("#resolveShellMessage") {
      it("should call reply callback on success ") {
        SparkKernelClient.resolveShell(Success(replyMessage), replyCallback, errorCallback)
        capturedReply shouldNot be(None)
        capturedReply.get shouldBe replyMessage
      }

      it("should call error callback with ShellException on failure") {
        SparkKernelClient.resolveShell(Failure(new RuntimeException), replyCallback, errorCallback)
        capturedError shouldNot be(None)
        capturedError.get.isInstanceOf[ShellException] shouldBe true
      }
    }
  }
}
