package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.utils.ExecutionCounter
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 */
class ExecuteRequestHandler(actorLoader: ActorLoader) extends Actor with ActorLogging {
  override def receive: Receive = {
    // sends execute request to interpreter
    case message: KernelMessage =>
      log.debug("Forwarding execute request")
      val executeRequest = Json.parse(message.contentString).as[ExecuteRequest]
      val executionCount = ExecutionCounter.incr(message.header.session)
      val relayActor = actorLoader.load(SystemActorType.Relay)
      //  This is a collection of common pieces that will be sent back in all reply message, use with .copy
      val messageReplySkeleton =  new KernelMessage( message.ids, "", null, message.header, Metadata(), null)

      //  Alert the clients the kernel is busy
      val busyMessage = messageReplySkeleton.copy(
        ids = Seq(MessageType.Status.toString),
        header = message.header.copy(msg_type = MessageType.Status.toString),
        contentString = KernelStatusBusy.toString
      )
      relayActor ! busyMessage

      //  Send a message to the clients saying we are executing something
      val executeInputMessage = messageReplySkeleton.copy(
        header = message.header.copy(msg_type = MessageType.ExecuteInput.toString),
        contentString = Json.toJson(new ExecuteInput(executeRequest.code, executionCount)).toString
      )
      relayActor ! executeInputMessage

      //  Alert the clients the kernel is busy
      val idleMessage = messageReplySkeleton.copy(
        ids = Seq(MessageType.Status.toString),
        header = message.header.copy(msg_type = MessageType.Status.toString),
        contentString = KernelStatusIdle.toString
      )

      // use future to keep message header in scope
      val future: Future[(ExecuteReply, ExecuteResult)] =
        ask(actorLoader.load(SystemActorType.Interpreter), executeRequest).mapTo[(ExecuteReply, ExecuteResult)]

      future.onComplete {
        case Success(tuple) =>
          log.debug("Sending Kernel messages to router")

          //  Send the reply message to the client
          val kernelReplyMessage = messageReplySkeleton.copy(
            header = message.header.copy(msg_type = MessageType.ExecuteReply.toString),
            contentString = Json.toJson(tuple._1.copy(execution_count = executionCount)).toString
          )
          relayActor ! kernelReplyMessage

          //  Send the result of the code execution
          val kernelResultMessage = messageReplySkeleton.copy(
            ids = Seq(MessageType.ExecuteResult.toString),
            header = message.header.copy(msg_type = MessageType.ExecuteResult.toString),
            contentString = Json.toJson(tuple._2.copy(execution_count = executionCount)).toString
          )
          relayActor ! kernelResultMessage

          //  Send the idle message
          relayActor ! idleMessage

        case Failure(error: Throwable) =>
          //  The reply error we will send back to the client
          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount, Option(error.getClass.getCanonicalName), Option(error.getMessage),
            Option(error.getStackTrace.map(_.toString).toList)
          )

          //  Send the error to the client
          relayActor ! messageReplySkeleton.copy(
            header = message.header.copy(msg_type = MessageType.ExecuteReply.toString),
            contentString = replyError
          )
          //  Send idle status to client
          relayActor ! idleMessage
      }
  }
}