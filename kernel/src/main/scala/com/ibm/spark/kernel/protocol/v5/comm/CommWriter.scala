package com.ibm.spark.kernel.protocol.v5.comm

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._

import java.io.Writer
import reflect.runtime.universe._

/**
 * Represents a basic writer used to communicate comm-related messages.
 *
 * @param actorLoader The actor loader to use for loading actors responsible for
 *                    communication
 * @param kmBuilder The kernel message builder used to construct kernel messages
 * @param commId The comm id associated with this writer
 */
class CommWriter(
  private val actorLoader: ActorLoader,
  private val kmBuilder: KMBuilder,
  private val commId: UUID
) extends Writer {

  private val MessageFieldName = "message"

  /**
   * Packages and sends an open message with the provided target and data.
   *
   * @param targetName The name of the target (used by the recipient)
   * @param data The optional data to send with the open message
   */
  def writeOpen(targetName: String, data: Data = Data()) =
    sendCommKernelMessage(CommOpen(commId, targetName, data))

  /**
   * Packages and sends a message with the provided data.
   *
   * @param data The data to send
   */
  def writeMsg(data: Data) = {
    require(data != null)

    sendCommKernelMessage(CommMsg(commId, data))
  }

  /**
   * Packages and sends a close message with the provided data.
   *
   * @param data The optional data to send with the close message
   */
  def writeClose(data: Data = Data()) =
    sendCommKernelMessage(CommClose(commId, data))

  /**
   * Writes the data as a new message, wrapped with a "message" JSON field.
   *
   * @param cbuf The array of characters to send
   * @param off The offset (0 is the start) of the array to use
   * @param len The number of characters to send
   */
  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
    val message = new String(cbuf.slice(off, len))
    val packedMessage = packageMessage(message)
    sendCommKernelMessage(packedMessage)
  }

  /**
   * Does nothing in this implementation.
   */
  override def flush(): Unit = {}

  /**
   * Sends a close message without any data.
   */
  override def close(): Unit = writeClose()

  /**
   * Packages a string message as a Comm message.
   *
   * @param message The string message to package
   *
   * @return The resulting CommMsg wrapper
   */
  private def packageMessage(message: String) =
    CommMsg(commId, Data(MessageFieldName -> message))

  /**
   * Sends the comm message (open/msg/close) to the actor responsible for
   * relaying messages.
   *
   * @param commContent The message to relay (will be packaged)
   *
   * @tparam T Either CommOpen, CommMsg, or CommClose
   */
  private def sendCommKernelMessage[
    T <: KernelMessageContent with CommContent
  ](commContent: T) = {
    val messageType = commContent match {
      case _: CommOpen  => MessageType.CommOpen
      case _: CommMsg   => MessageType.CommMsg
      case _: CommClose => MessageType.CommClose
      case _            => throw new Throwable("Invalid kernel message type!")
    }
    actorLoader.load(SystemActorType.KernelMessageRelay) !
      kmBuilder.withHeader(messageType).withContentString(commContent).build
  }
}
