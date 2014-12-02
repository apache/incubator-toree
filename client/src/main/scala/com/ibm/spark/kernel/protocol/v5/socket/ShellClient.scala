package com.ibm.spark.kernel.protocol.v5.socket

import akka.actor.Actor
import akka.util.Timeout
import akka.zeromq.ZMQMessage
import com.ibm.spark.kernel.protocol.v5.{Utilities, KernelMessage, UUID}
import com.ibm.spark.kernel.protocol.v5.Utilities._
import com.ibm.spark.kernel.protocol.v5.client.execution.{DeferredExecution, DeferredExecutionManager}
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply

import com.ibm.spark.utils.LogLike
import scala.concurrent.duration._

/**
 * The client endpoint for Shell messages specified in the IPython Kernel Spec
 * @param socketFactory A factory to create the ZeroMQ socket connection
 */
class ShellClient(socketFactory: ClientSocketFactory) extends Actor with LogLike {
  logger.debug("Created shell client actor")
  implicit val timeout = Timeout(21474835.seconds)

  val socket = socketFactory.ShellClient(context.system, self)

  def receiveExecuteReply(parentId:String, kernelMessage: KernelMessage): Unit = {
    val deOption: Option[DeferredExecution] = DeferredExecutionManager.get(parentId)
    deOption match {
      case None =>
        logger.warn(s"No deferred execution for parent id ${parentId}")
      case Some(de) =>
        Utilities.parseAndHandle(kernelMessage.contentString,
          ExecuteReply.executeReplyOkReads, (er: ExecuteReply) => de.resolveReply(er))
    }
  }

  override def receive: Receive = {
    // from shell
    case message: ZMQMessage =>
      logger.debug("Received shell kernel message.")
      val kernelMessage: KernelMessage = message
      logger.trace(s"Kernel message is ${kernelMessage}")
      receiveExecuteReply(message.parentHeader.msg_id,kernelMessage)

    // from handler
    case message: KernelMessage =>
      logger.trace(s"Sending kernel message ${message}")
      val zmq: ZMQMessage = message
      socket ! zmq
  }
}
