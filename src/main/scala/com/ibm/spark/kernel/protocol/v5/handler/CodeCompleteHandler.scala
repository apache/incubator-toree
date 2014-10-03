package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.utils.LogLike
import play.api.data.validation.ValidationError
import play.api.libs.json.{Json, JsPath}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask

import scala.util.{Failure, Success}

class CodeCompleteHandler(actorLoader: ActorLoader) extends Actor with LogLike {

  override def receive: Receive = {
    case message: KernelMessage =>
      logger.info("Received CompleteRequest")
      val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
      logger.debug(s"contentString is ${message.contentString}")
      Json.parse(message.contentString).validate[CompleteRequest].fold(
        (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
          logger.error("Could not parse JSON for complete request!!!!!!!!!!!!!")
          throw new Throwable("Parse error in CodeCompleteHandler")
        },
        (completeRequest: CompleteRequest) => {
          logger.debug("Completion request being asked to interpreterActor")
          val codeCompleteFuture = ask(interpreterActor, completeRequest).mapTo[(Int, List[String])]
          codeCompleteFuture.onComplete {
            case Success(tuple) =>
              //  Construct a CompleteReply
              val reply = CompleteReplyOk(tuple._2, completeRequest.cursor_pos, tuple._1, Metadata())
              //  Send the CompleteReply to the Relay actor
              logger.debug("Sending complete reply to relay actor")
              actorLoader.load(SystemActorType.KernelMessageRelay) !
                message.copy(
                  //header = message.header.copy(msg_type = MessageType.CompleteReply.toString),
                  header = HeaderBuilder.create(MessageType.CompleteReply.toString),
                  parentHeader = message.header,
                  contentString = Json.toJson(reply).toString
                )
              actorLoader.load(SystemActorType.StatusDispatch) ! Tuple2(KernelStatusType.Idle, message.header)
            case _ =>
              new Exception("Parse error in CodeCompleteHandler")
          }
        }
      )
  }

}
