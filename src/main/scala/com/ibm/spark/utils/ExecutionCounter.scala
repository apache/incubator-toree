package com.ibm.spark.utils

import com.ibm.spark.kernel.protocol.v5.UUID
import scala.collection.concurrent._

/**
 * A class to keep track of execution counts for sessions.
 */
object ExecutionCounter {
  private val executionCounts: Map[UUID, Int] = TrieMap[UUID, Int]()

  /**
   * This function will increase, by 1, an integer value associated with the given key. The incremented value
   * will be returned. If the key has no value associated value, 1 will be returned.
   * @param key The key for incrementing the value.
   * @return The incremented value
   */
  def incr(key: UUID): Int = {
    (executionCounts += (key -> (executionCounts.getOrElse(key, 0) + 1))) (key)
  }
}
