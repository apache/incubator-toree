package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorLogging, Actor}
import com.ibm.spark.kernel.protocol.v5.{ActorLoader, KernelMessage}
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import play.api.libs.json.Json

/**
 * Receives an ExecuteRequest KernelMessage and forwards the ExecuteRequest
 * to the interpreter actor.
 */
class ExecuteRequestHandler(actorLoader: ActorLoader) extends Actor with ActorLogging {
  override def receive: Receive = {
    case message: KernelMessage =>
      log.debug("Forwarding execute request")

      val executeRequest = Json.parse(message.contentString).as[ExecuteRequest]
      actorLoader.loadInterpreterActor() ! executeRequest
  }
}