package com.ibm.spark.kernel.api

import java.io.{InputStream, OutputStream}

import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.kernel.protocol.v5.{KMBuilder, KernelMessage}
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.stream.{KernelOutputStream, KernelInputStream}
import com.typesafe.config.Config

/**
 * Represents the methods available to stream data from the kernel to the
 * client.
 *
 * @param actorLoader The actor loader to use when retrieve actors needed for
 *                    object creation
 * @param parentMessage The parent message to use when needed for object
 *                      creation
 * @param kernelMessageBuilder The builder to use when constructing kernel
 *                             messages inside objects created
 */
class FactoryMethods(
  private val actorLoader: ActorLoader,
  private val parentMessage: KernelMessage,
  private val kernelMessageBuilder: KMBuilder
) extends FactoryMethodsLike {
  private[api] val kmBuilder = kernelMessageBuilder.withParent(parentMessage)

  /**
   * Creates a new kernel input stream.
   *
   * @param prompt The text to use as a prompt
   * @param password If true, should treat input as a password field
   *
   * @return The new KernelInputStream instance
   */
  override def newKernelInputStream(
    prompt: String = KernelInputStream.DefaultPrompt,
    password: Boolean = KernelInputStream.DefaultPassword
  ): InputStream = {
    new KernelInputStream(
      actorLoader,
      kmBuilder.withIds(parentMessage.ids),
      prompt = prompt,
      password = password
    )
  }

  /**
   * Creates a new kernel output stream.
   *
   * @param streamType The type of output stream (stdout/stderr)
   * @param sendEmptyOutput If true, will send message even if output is empty
   *
   * @return The new KernelOutputStream instance
   */
  override def newKernelOutputStream(
    streamType: String = KernelOutputStream.DefaultStreamType,
    sendEmptyOutput: Boolean = KernelOutputStream.DefaultSendEmptyOutput
  ): OutputStream = {
    new v5.stream.KernelOutputStream(
      actorLoader,
      kmBuilder,
      com.ibm.spark.global.ScheduledTaskManager.instance,
      streamType = streamType,
      sendEmptyOutput = sendEmptyOutput
    )
  }
}
