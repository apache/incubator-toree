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

package org.apache.toree.kernel.protocol.v5.client.execution

import org.apache.toree.kernel.protocol.v5.UUID
import org.apache.toree.utils.LogLike

import scala.collection.concurrent.{Map, TrieMap}

object DeferredExecutionManager extends LogLike{
  private val executionMap: Map[UUID, DeferredExecution] = TrieMap[UUID, DeferredExecution]()
  
  def add(id: UUID, de: DeferredExecution): Unit = executionMap += (id -> de)

  def get(id: UUID): Option[DeferredExecution] = executionMap.get(id)

  def remove(de: DeferredExecution): Unit = {
    val optionalDE: Option[(UUID, DeferredExecution)] = executionMap.find {
      case (id: UUID, searchedDe: DeferredExecution) => {
        de.eq(searchedDe)
    }}
    optionalDE match {
      case None =>
        logger.warn("Searched and did not find deferred execution!")
      case Some((id, foundDe)) =>
        executionMap.remove(id)
    }
  }

}
