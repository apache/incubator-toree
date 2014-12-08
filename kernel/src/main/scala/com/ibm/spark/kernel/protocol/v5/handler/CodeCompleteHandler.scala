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
    Utilities.parseAndHandle(
      kernelMessage.contentString,
      CompleteRequest.completeRequestReads,
      completeRequest(kernelMessage, _ : CompleteRequest)
    )
  }

  private def completeRequest(km: KernelMessage, cr: CompleteRequest):
                              Future[(Int, List[String])] = {
    val interpreterActor = actorLoader.load(SystemActorType.Interpreter)
    val codeCompleteFuture = ask(interpreterActor, cr).mapTo[(Int, List[String])]
    codeCompleteFuture.onComplete {
      case Success(tuple) =>
        val reply = CompleteReplyOk(tuple._2, cr.cursor_pos,
                                    tuple._1, Metadata())
        logKernelMessageAction("Sending code complete reply for", km)
        actorLoader.load(SystemActorType.KernelMessageRelay) !
          km.copy(
            header = HeaderBuilder.create(MessageType.CompleteReply.toString),
            parentHeader = km.header,
            contentString = Json.toJson(reply).toString
          )
      case _ =>
        new Exception("Parse error in CodeCompleteHandler")
    }
    codeCompleteFuture
  }
}
