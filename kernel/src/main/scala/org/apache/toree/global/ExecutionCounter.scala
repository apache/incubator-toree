/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.global

import org.apache.toree.kernel.protocol.v5.UUID

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
