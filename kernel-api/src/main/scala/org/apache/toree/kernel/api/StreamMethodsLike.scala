package org.apache.toree.kernel.api

/**
 * Represents the methods available to stream data from the kernel to the
 * client.
 */
trait StreamMethodsLike {
  /**
   * Sends all text provided as one stream message to the client.
   * @param text The text to wrap in a stream message
   */
  def sendAll(text: String): Unit
}
