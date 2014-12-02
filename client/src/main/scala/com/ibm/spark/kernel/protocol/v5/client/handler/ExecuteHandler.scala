package com.ibm.spark.kernel.protocol.v5.client.handler

import akka.actor.Actor
import akka.util.Timeout
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.client.execution.{ExecuteRequestTuple, DeferredExecutionManager}
import com.ibm.spark.utils.LogLike
import scala.concurrent.duration._

/**
 * Actor for handling client execute request and reply messages
 */
class ExecuteHandler(actorLoader: ActorLoader) extends Actor with LogLike {
  implicit val timeout = Timeout(21474835.seconds)

  override def receive: Receive = {
    case reqTuple: ExecuteRequestTuple =>
      // create message to send to shell
      val km: KernelMessage = Utilities.toKernelMessage(reqTuple.request)
      //  Register the execution for this message id with the manager
      DeferredExecutionManager.add(km.header.msg_id,reqTuple.de)

      // send the message to the ShellClient
      val shellClient = actorLoader.load(SocketType.ShellClient)
      shellClient ! km
  }
}