package com.ibm.spark.kernel.protocol.v5.stream

import java.io.{OutputStream}
import java.nio.charset.Charset
import java.util.UUID

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.{StreamContent, ExecuteResult}
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, ActorLoader}
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer

class KernelMessageStream(
  actorLoader: ActorLoader,
  skeletonKernelMessage: KernelMessage
) extends OutputStream {
  private val EncodingType = Charset.forName("UTF-8")
  private var internalBytes: ListBuffer[Byte] = ListBuffer()

  /**
   * Takes the current byte array contents in memory, packages them up into a
   * KernelMessage, and sends the message to the KernelMessageRelay.
   */
  override def flush(): Unit = {
    val contents = new String(internalBytes.toArray, EncodingType)
    val streamContent = StreamContent(
      "stdout", contents
    )

    // Build our kernel message with the current byte array contents
    val kernelMessage = skeletonKernelMessage.copy(
      ids = Seq(MessageType.Stream.toString),
      header = skeletonKernelMessage.parentHeader.copy(
        msg_type = MessageType.Stream.toString,
        msg_id = UUID.randomUUID().toString
      ),
      contentString = Json.toJson(streamContent).toString
    )

    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelMessage

    // Ensure any underlying implementation is processed
    super.flush()

    // Clear the internal buffer
    internalBytes.clear()
  }

  /**
   * Adds the specified byte to the end of the internal buffer. The most
   * significant 24 bits are ignored. Only the least significant 8 bits
   * are appended.
   * @param b The byte whose least significant 8 bits are to be appended
   */
  override def write(b: Int): Unit = {
    internalBytes += b.toByte

    // Attempt a flush if the provided byte was a newline
    if (b.toChar == '\n') flush()
  }
}
