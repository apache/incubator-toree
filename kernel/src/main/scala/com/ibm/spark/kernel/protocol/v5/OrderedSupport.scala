package com.ibm.spark.kernel.protocol.v5

import akka.actor.{Stash, Actor}
import com.ibm.spark.utils.LogLike

/**
 * A trait to enforce ordered processing for messages of particular types.
 */
trait OrderedSupport extends Actor with Stash with LogLike {
  /**
   * Executes instead of the default receive when the Actor has begun
   * processing. Stashes incoming messages of particular types, defined by
   * {@link #orderedTypes() orderedTypes} function, for later processing. Uses
   * the default receive method for all other types. Upon receiving a
   * FinishedProcessing message, resumes processing all messages with the
   * default receive.
   * @return
   */
  def waiting : Receive = {
    case FinishedProcessing =>
      context.unbecome()
      unstashAll()
    case aVal: Any if (orderedTypes().contains(aVal.getClass)) =>
      logger.trace(s"Stashing message ${aVal} of type ${aVal.getClass}.")
      stash()
    case aVal: Any =>
      logger.trace(s"Forwarding message ${aVal} of type ${aVal.getClass} " +
        "to default receive.")
      receive(aVal)
  }

  /**
   * Suspends the default receive method for types defined by the
   * {@link #orderedTypes() orderedTypes} function.
   */
  def startProcessing(): Unit = {
    logger.debug("Actor is in processing state and will stash messages of " +
      s"types: ${orderedTypes.mkString(" ")}")
    context.become(waiting, discardOld = false)
  }

  /**
   * Resumes the default receive method for all message types.
   */
  def finishedProcessing(): Unit = {
    logger.debug("Actor is no longer in processing state.")
    self ! FinishedProcessing
  }

  /**
   * Defines the types that will be stashed by {@link #waiting() waiting}
   * while the Actor is in processing state.
   * @return
   */
  def orderedTypes(): Seq[Class[_]]

  case object FinishedProcessing
}
