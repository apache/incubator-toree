package com.ibm.spark.utils

import com.ibm.spark.kernel.protocol.v5.UUID
import scala.collection.concurrent._

object ExecutionCounter {
  private val executionCounts: Map[UUID, Int] = TrieMap[UUID, Int]()
  def incr(key: UUID): Int = {
    (executionCounts += (key -> (executionCounts.getOrElse(key, 0) + 1))) (key)
  }
}
