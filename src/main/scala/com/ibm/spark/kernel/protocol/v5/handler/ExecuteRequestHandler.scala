package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{Actor, ActorSelection}
import akka.pattern.ask
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.stream.KernelMessageStream
import com.ibm.spark.utils.{ExecutionCounter, LogLike}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 */
class ExecuteRequestHandler(actorLoader: ActorLoader) extends Actor with LogLike {

  override def receive: Receive = {
    // sends execute request to interpreter
    case message: KernelMessage =>
      logger.debug("Forwarding execute request")
      val executionCount = ExecutionCounter.incr(message.header.session)
      val relayActor = actorLoader.load(SystemActorType.KernelMessageRelay)
      //  This is a collection of common pieces that will be sent back in all reply message, use with .copy
      val messageReplySkeleton =  new KernelMessage( message.ids, "", null,  message.header, Metadata(), null)

      Json.parse(message.contentString).validate[ExecuteRequest].fold(
        (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
          val validationErrors: List[String] = List()
          for (path <- invalid) {
            validationErrors :+ s"JSPath ${path._1} has error: ${path._2(0).toString}"
          }
          logger.error("Validation errors when parsing ExecuteRequest:")
          logger.error(s"$validationErrors")
          val replyError: ExecuteReply = ExecuteReplyError(
            executionCount, Option("JsonParseException"), Option("Error parsing fields"),
            Option(validationErrors)
          )
          relayErrorMessages(relayActor, replyError, message.header, messageReplySkeleton)
        },
        (executeRequest: ExecuteRequest) => {
          //  Alert the clients the kernel is busy
          actorLoader.load(SystemActorType.StatusDispatch) ! Tuple2(KernelStatusType.Busy, message.header)

          //  Send a message to the clients saying we are executing something
          val executeInputMessage = messageReplySkeleton.copy(
            header = message.header.copy(msg_type = MessageType.ExecuteInput.toString),
            contentString = Json.toJson(new ExecuteInput(executeRequest.code, executionCount)).toString
          )
          relayActor ! executeInputMessage

          val executeFuture = ask(
            actorLoader.load(SystemActorType.ExecuteRequestRelay),
            (executeRequest, new KernelMessageStream(actorLoader, messageReplySkeleton))
          ).mapTo[(ExecuteReply, ExecuteResult)]

          executeFuture.onComplete {
            case Success(tuple) =>
              logger.debug("Sending Kernel messages to router")

              //  Send the reply message to the client
              val kernelReplyMessage = messageReplySkeleton.copy(
                header = message.header.copy(msg_type = MessageType.ExecuteReply.toString),
                contentString = Json.toJson(tuple._1.copy(execution_count = executionCount)).toString
              )
              relayActor ! kernelReplyMessage

              //  Send the result of the code execution
              if (tuple._2.hasContent) {
                val kernelResultMessage = messageReplySkeleton.copy(
                  ids = Seq(MessageType.ExecuteResult.toString),
                  header = message.header.copy(msg_type = MessageType.ExecuteResult.toString),
                  contentString = Json.toJson(tuple._2.copy(execution_count = executionCount)).toString
                )
                relayActor ! kernelResultMessage
              }

            case Failure(error: Throwable) =>
              //  Send the error to the client on the Shell socket
              val replyError: ExecuteReply = ExecuteReplyError(
                executionCount, Option(error.getClass.getCanonicalName), Option(error.getMessage),
                Option(error.getStackTrace.map(_.toString).toList)
              )
              relayErrorMessages(relayActor, replyError, message.header, messageReplySkeleton)
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
   */
  def relayErrorMessages(relayActor: ActorSelection, replyError: ExecuteReply, headerSkeleton: Header,
                         messageReplySkeleton: KernelMessage) {
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
    actorLoader.load(SystemActorType.StatusDispatch) ! Tuple2(KernelStatusType.Idle, headerSkeleton)
  }
}