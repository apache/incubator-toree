package com.ibm.spark.kernel.protocol.v5.relay

import java.io.OutputStream

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.util.Timeout
import com.ibm.spark.interpreter.{ExecuteError, ExecuteOutput}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.magic.{ExecuteMagicMessage, ValidateMagicMessage}
import com.ibm.spark.utils.LogLike
import akka.pattern._
import scala.concurrent.Future
import scala.concurrent.duration._
import com.ibm.spark.kernel.protocol.v5.content._

case class ExecuteRequestRelay(actorLoader: ActorLoader)
  extends Actor with LogLike
{
  import context._
  implicit val timeout = Timeout(100000.days)

  /**
   * Packages the response into an ExecuteReply,ExecuteResult tuple.
   * @param future The future containing either the output or error
   * @return The tuple representing the proper response
   */
  private def packageFutureResponse(
    future: Future[Either[ExecuteOutput, ExecuteError]]
  ) = future.map { value =>
    if (value.isLeft) {
      val output = value.left.get
      (
        ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
        ExecuteResult(1, Data("text/plain" -> output), Metadata())
      )
    } else {
      val error = value.right.get
      (
        ExecuteReplyError(1, Some(error.name), Some(error.value),
          Some(error.stackTrace)),
        ExecuteResult(1, Data("text/plain" -> error.toString), Metadata())
      )
    }
  }

  override def receive: Receive = {
    case (executeRequest: ExecuteRequest, outputStream: OutputStream) =>
      val magicManager = actorLoader.load(SystemActorType.MagicManager)
      val futureIsMagic =
        magicManager ? ValidateMagicMessage(executeRequest.code)

      // Store our old sender so we don't lose it in the callback
      // NOTE: Should point back to our KernelMessageRelay
      val oldSender = sender

      // TODO: Investigate how to use the success to set the future ask
      //       destination without running duplicate code
      futureIsMagic.onSuccess {
        case true =>
          val magicMessage = ExecuteMagicMessage(executeRequest.code)
          val future  =
            (magicManager ? ((magicMessage, outputStream)))
              .mapTo[Either[ExecuteOutput, ExecuteError]]

          packageFutureResponse(future) pipeTo oldSender
        case false =>
          val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
          val future =
            (interpreterActor ? ((executeRequest, outputStream)))
              .mapTo[Either[ExecuteOutput, ExecuteError]]

          packageFutureResponse(future) pipeTo oldSender
      }
  }
}
