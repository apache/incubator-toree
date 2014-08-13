package com.ibm.spark.kernel.protocol.v5.dispatch

import java.util.UUID

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5.content.KernelStatus
import com.ibm.spark.kernel.protocol.v5.KernelStatusType.KernelStatusType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

class StatusDispatch(actorLoader: ActorLoader) extends Actor with LogLike {
  private def sendStatusMessage(kernelStatus: KernelStatusType) {
    //  Create the status message and send it to the relay
    actorLoader.load(SystemActorType.Relay) ! KernelMessage(
      Seq(MessageType.Status.toString),
      "",
      Header(UUID.randomUUID().toString, "","", MessageType.Status.toString,""),
      EmptyParentHeader,
      Metadata(),
      Json.toJson(KernelStatus(kernelStatus.toString)).toString
    )
  }

  override def receive: Receive = {
    case status: KernelStatusType => {
      sendStatusMessage( status )
    }
  }
}
