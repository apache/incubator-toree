package com.ibm.spark.client

import com.ibm.spark.kernel.protocol.v5.UUID
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult

import scala.collection.concurrent.{Map, TrieMap}

/**
 * Created by Chris on 8/8/14.
 */
object CallbackMap {
  private val callbacks: Map[UUID, ExecuteResult => Any] = TrieMap[UUID, ExecuteResult => Any]()

  def put(key: UUID, callback: ExecuteResult => Any): Unit = {
    callbacks += (key -> callback)
  }

  def get(key: UUID): Option[ExecuteResult => Any] = {
    callbacks.get(key)
  }

  def remove(key: UUID): Unit = {
    callbacks.remove(key)
  }
}
