package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorSelection, Actor, ActorLogging}
import akka.pattern.ask
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.utils.ExecutionCounter
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}

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
      val executionCount = ExecutionCounter.incr(message.header.session)
      val relayActor = actorLoader.load(SystemActorType.Relay)
      //  This is a collection of common pieces that will be sent back in all reply message, use with .copy
      val messageReplySkeleton =  new KernelMessage( message.ids, "", null,  message.header, Metadata(), null)
      //  All code paths in this function will need to send the idle status
      val idleMessage = messageReplySkeleton.copy(
        ids = Seq(MessageType.Status.toString),
        header = message.header.copy(msg_type = MessageType.Status.toString),
        contentString = KernelStatusIdle.toString
      )

      Json.parse(message.contentString).validate[ExecuteRequest].fold(
        (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
          println("Validation errors when parsing ExecuteRequest")
          //  Determine what to do here
          val output: List[String] = List()
          for (path <- invalid) {
            output :+ s"JSPath ${path._1} has error: ${path._2(0).toString}"
          }

          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount, Option("JsonParseException"), Option("Error parsing fields"),
            Option(output)
          )
          relayErrorMessages(relayActor, replyError, message.header, messageReplySkeleton, idleMessage)
        },
        (executeRequest: ExecuteRequest) => {
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
              //  Send the error to the client on the Shell socket
              val replyError: ExecuteReply = ExecuteReplyError(
                executionCount, Option(error.getClass.getCanonicalName), Option(error.getMessage),
                Option(error.getStackTrace.map(_.toString).toList)
              )
              relayErrorMessages(relayActor, replyError, message.header, messageReplySkeleton, idleMessage)
          }
        }
      )
  }

  /**
   * Create a common method to relay errors based on a ExecuteReplyError
   * @param relayActor The relay to send messages through
   * @param replyError The reply error to build the error messages from
   * @param headerSkeleton A skeleton for the reply headers (Everything should be filled in except msg_type)
   * @param messageReplySkeleton A skeleton to build messages from (Everything should be filled in except header, contentString)
   * @param idleMessage A generic idle message for a particular ExecuteRequest
   */
  def relayErrorMessages(relayActor: ActorSelection, replyError: ExecuteReply, headerSkeleton: Header,
                         messageReplySkeleton: KernelMessage, idleMessage: KernelMessage) {
    relayActor ! messageReplySkeleton.copy(
      header = headerSkeleton.copy(msg_type = MessageType.ExecuteReply.toString),
      contentString = replyError
    )

    //  Send the error to the client on the IOPub socket
    val errorContent: ErrorContent =  ErrorContent(
      replyError.ename.get, replyError.evalue.get, replyError.traceback.get
    )

    relayActor ! messageReplySkeleton.copy(
      header = headerSkeleton.copy(msg_type = MessageType.Error.toString), contentString = errorContent
    )

    //  Send idle status to client
    relayActor ! idleMessage
  }
}