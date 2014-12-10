package com.ibm.spark.kernel.protocol.v5.handler

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.utils.MessageLogSupport

import scala.concurrent.Future

abstract class BaseHandler(actorLoader: ActorLoader) extends OrderedSupport
  with MessageLogSupport {
  /**
   * Implements the receive method, sending a busy message out before
   * processing the message and sending an idle message out once finished.
   *
   * @return The Akka partial function n
   */
  final def receive = {
    case kernelMessage: KernelMessage =>
      startProcessing()
      // Send the busy message before we process the message
      logKernelMessageAction("Sending Busy message for", kernelMessage)
      actorLoader.load(SystemActorType.StatusDispatch) !
        Tuple2(KernelStatusType.Busy, kernelMessage.header)

      // Process the message
      logKernelMessageAction("Processing", kernelMessage)
      import scala.concurrent.ExecutionContext.Implicits.global
      val processFuture = process(kernelMessage)

      // Send the idle message since message has been processed
      logKernelMessageAction("Sending Idle message for", kernelMessage)
      processFuture onComplete {
        case _ =>
          actorLoader.load(SystemActorType.StatusDispatch) !
            Tuple2(KernelStatusType.Idle, kernelMessage.parentHeader)
          finishedProcessing()
      }
  }

  override def orderedTypes() : Seq[Class[_]] = {Seq(classOf[KernelMessage])}

  /**
   * Processes the provided kernel message.
   *
   * @param kernelMessage The kernel message instance to process
   */
  def process(kernelMessage: KernelMessage): Future[_]
}
