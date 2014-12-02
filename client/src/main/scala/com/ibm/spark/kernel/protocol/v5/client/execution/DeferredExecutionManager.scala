package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.UUID
import com.ibm.spark.utils.LogLike

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
