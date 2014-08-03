package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteInput, ExecuteReply, ExecuteRequest, ExecuteResult}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 */
class ExecuteRequestHandler(actorLoader: ActorLoader) extends Actor with ActorLogging {
  implicit val timeout = Timeout(5.seconds)

  override def receive: Receive = {
    // sends execute request to interpreter
    case message: KernelMessage =>
      log.debug("Forwarding execute request")
      val executeRequest = Json.parse(message.contentString).as[ExecuteRequest]

      val inputHeader = message.header.copy(msg_type = MessageType.ExecuteInput.toString)

      val executeInputMessage = new KernelMessage(
        message.ids,
        "",
        inputHeader,
        message.header,
        Metadata(),
        Json.toJson(new ExecuteInput(executeRequest.code,1)).toString
      )

      actorLoader.loadRelayActor() ! executeInputMessage



      // use future to keep message header in scope
      val future: Future[(ExecuteReply, ExecuteResult)] =
        ask(actorLoader.loadInterpreterActor(), executeRequest).mapTo[(ExecuteReply, ExecuteResult)]

      future.onComplete {
        case Success(tuple) =>
          log.debug("Sending Kernel messages to router")

          val replyHeader = message.header.copy(msg_type = MessageType.ExecuteReply.toString)

          val kernelReplyMessage = new KernelMessage(
            message.ids,
            "",
            replyHeader,
            message.header,
            Metadata(),
            Json.toJson(tuple._1).toString
          )

          val resultHeader = message.header.copy(msg_type = MessageType.ExecuteResult.toString)
          val kernelResultMessage = new KernelMessage(
            Seq(MessageType.ExecuteResult.toString),
            "",
            resultHeader,
            message.header,
            Metadata(),
            Json.toJson(tuple._2).toString
          )

          val relayActor = actorLoader.loadRelayActor()
          relayActor ! kernelReplyMessage
          relayActor ! kernelResultMessage

        case Failure(_) =>
          // todo send error message to relay
      }
  }
}