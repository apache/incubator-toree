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
package org.apache.toree.kernel.interpreter.pyspark

import org.apache.toree.interpreter.broker.producer.{StandardSQLContextProducer, StandardJavaSparkContextProducer, SQLContextProducerLike, JavaSparkContextProducerLike}
import org.apache.toree.interpreter.broker.{BrokerState, BrokerBridge}
import org.apache.toree.kernel.api.KernelLike
import org.apache.spark.SparkContext

/**
 * Represents constants for the PySpark bridge.
 */
object PySparkBridge {
  /** Represents the maximum amount of code that can be queued for Python. */
  val MaxQueuedCode = 500

  /**
   * Creates a new PySparkBridge instance.
   *
   * @param brokerState The container of broker state to expose
   * @param kernel The kernel API to expose through the bridge
   *
   * @return The new PySpark bridge
   */
  def apply(
    brokerState: BrokerState,
    kernel: KernelLike
  ): PySparkBridge = {
    new PySparkBridge(
      _brokerState = brokerState,
      _kernel = kernel
    ) with StandardJavaSparkContextProducer with StandardSQLContextProducer
  }
}

/**
 * Represents the API available to PySpark to act as the bridge for data
 * between the JVM and Python.
 *
 * @param _brokerState The container of broker state to expose
 * @param _kernel The kernel API to expose through the bridge
 */
class PySparkBridge private (
  private val _brokerState: BrokerState,
  private val _kernel: KernelLike
) extends BrokerBridge(_brokerState, _kernel) {
  override val brokerName: String = "PySpark"
}
