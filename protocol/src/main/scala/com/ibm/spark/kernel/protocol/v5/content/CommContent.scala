package com.ibm.spark.kernel.protocol.v5.content

import com.ibm.spark.kernel.protocol.v5.KernelMessageContent

/**
 * Represents a series of subclasses of KernelMessageContent that embodies the
 * Comm content types.
 */
trait CommContent {
  this: KernelMessageContent =>
}
