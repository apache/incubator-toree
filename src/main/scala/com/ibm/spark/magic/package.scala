package com.ibm.spark

import com.ibm.spark.kernel.protocol.v5.Data

package object magic {
  /**
   * Represents the output of a magic execution.
   */
  type MagicOutput = Data
  val MagicOutput = Data
}
