package com.ibm.spark.kernel.protocol.v5.handler

import akka.pattern.ask
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.utils.{MessageLogSupport, LogLike}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class CodeCompleteHandler(actorLoader: ActorLoader)
  extends BaseHandler(actorLoader) with MessageLogSupport
{
  override def process(kernelMessage: KernelMessage): Future[_] = {
    logKernelMessageAction("Generating code completion for", kernelMessage)
    val interpreterActor = actorLoader.load(SystemActorType.Interpreter)

    // TODO refactor using a function like the client's Utilities.parseAndHandle
    Json.parse(kernelMessage.contentString).validate[CompleteRequest].fold(
      (invalid: Seq[(JsPath, Seq[ValidationError])]) => {
        logger.error("Could not parse JSON for complete request!")
        throw new Throwable("Parse error in CodeCompleteHandler")
      },
      (completeRequest: CompleteRequest) => {
        val codeCompleteFuture = ask(interpreterActor, completeRequest).mapTo[(Int, List[String])]
        codeCompleteFuture.onComplete {
          case Success(tuple) =>
            //  Construct a CompleteReply
            val reply = CompleteReplyOk(tuple._2, completeRequest.cursor_pos, tuple._1, Metadata())
            //  Send the CompleteReply to the Relay actor
            logger.debug("Sending complete reply to relay actor")
            actorLoader.load(SystemActorType.KernelMessageRelay) !
              kernelMessage.copy(
                //header = kernelMessage.header.copy(msg_type = MessageType.CompleteReply.toString),
                header = HeaderBuilder.create(MessageType.CompleteReply.toString),
                parentHeader = kernelMessage.header,
                contentString = Json.toJson(reply).toString
              )
          case _ =>
            new Exception("Parse error in CodeCompleteHandler")
        }
        codeCompleteFuture
      }
    )
  }
}
