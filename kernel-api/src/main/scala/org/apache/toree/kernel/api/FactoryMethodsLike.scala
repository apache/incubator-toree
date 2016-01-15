package org.apache.toree.kernel.api

import java.io.{InputStream, OutputStream}

/**
 * Represents the methods available to create objects related to the kernel.
 */
trait FactoryMethodsLike {
  /**
   * Creates a new kernel output stream.
   *
   * @param streamType The type of output stream (stdout/stderr)
   * @param sendEmptyOutput If true, will send message even if output is empty
   *
   * @return The new KernelOutputStream instance
   */
  def newKernelOutputStream(
    streamType: String,
    sendEmptyOutput: Boolean
  ): OutputStream

  /**
   * Creates a new kernel input stream.
   *
   * @param prompt The text to use as a prompt
   * @param password If true, should treat input as a password field
   *
   * @return The new KernelInputStream instance
   */
  def newKernelInputStream(
    prompt: String,
    password: Boolean
  ): InputStream
}
