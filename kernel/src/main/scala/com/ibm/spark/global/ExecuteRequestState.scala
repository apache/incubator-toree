package com.ibm.spark.global

import com.ibm.spark.kernel.protocol.v5.KernelMessage

/**
 * Represents the state of the kernel messages being received containing
 * execute requests.
 */
object ExecuteRequestState {
  private var _lastKernelMessage: Option[KernelMessage] = None

  /**
   * Processes the incoming kernel message and updates any associated state.
   *
   * @param kernelMessage The kernel message to process
   */
  def processIncomingKernelMessage(kernelMessage: KernelMessage) =
    _lastKernelMessage = Some(kernelMessage)

  /**
   * Returns the last kernel message funneled through the KernelMessageRelay
   * if any.
   *
   * @return Some KernelMessage instance if the relay has processed one,
   *         otherwise None
   */
  def lastKernelMessage: Option[KernelMessage] = _lastKernelMessage

  /**
   * Resets the state of the ExecuteRequestState to the default.
   */
  def reset() = _lastKernelMessage = None
}
