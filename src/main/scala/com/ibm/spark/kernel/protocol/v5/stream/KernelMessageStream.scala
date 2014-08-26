package com.ibm.spark.kernel.protocol.v5.stream

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.kernel.protocol.v5.{KernelMessage, ActorLoader}
import play.api.libs.json.Json

class KernelMessageStream(
  actorLoader: ActorLoader,
  skeletonKernelMessage: KernelMessage,
  executionCount: Int
) extends ByteArrayOutputStream {

  /**
   * Takes the current byte array contents in memory, packages them up into a
   * KernelMessage, and sends the message to the KernelMessageRelay.
   */
  override def flush(): Unit = {
    val contents = this.toString(Charset.defaultCharset().displayName()).trim
    val executeResult = ExecuteResult(
      executionCount,
      Data("text/plain" -> contents),
      Metadata()
    )

    // Build our kernel message with the current byte array contents
    val kernelMessage = skeletonKernelMessage.copy(
      ids = Seq(MessageType.ExecuteResult.toString),
      header = skeletonKernelMessage.parentHeader.copy(
        msg_type = MessageType.ExecuteResult.toString
      ),
      contentString = Json.toJson(executeResult).toString
    )

    actorLoader.load(SystemActorType.KernelMessageRelay) ! kernelMessage

    // Ensure any underlying implementation is processed
    super.flush()
  }
}
