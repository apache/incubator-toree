package com.ibm.spark.kernel.protocol.v5.dispatch

import akka.actor.Actor
import com.ibm.spark.kernel.protocol.v5.KernelStatusType.KernelStatusType
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelStatus
import com.ibm.spark.utils.LogLike
import play.api.libs.json.Json

class StatusDispatch(actorLoader: ActorLoader) extends Actor with LogLike {
  private def sendStatusMessage(kernelStatus: KernelStatusType, parentHeader: Header) {
    //  Create the status message and send it to the relay
    actorLoader.load(SystemActorType.KernelMessageRelay) ! KernelMessage(
      Seq(MessageType.Status.toString),
      "",
      HeaderBuilder.create(MessageType.Status.toString),
      parentHeader,
      Metadata(),
      Json.toJson(KernelStatus(kernelStatus.toString)).toString
    )
  }

  override def receive: Receive = {
    case (status: KernelStatusType, null) =>
      sendStatusMessage(status, null)

    case (status: KernelStatusType, parentHeader: Header) =>
      sendStatusMessage( status, parentHeader )

    case status: KernelStatusType =>
      sendStatusMessage(status , HeaderBuilder.empty)
  }
}
