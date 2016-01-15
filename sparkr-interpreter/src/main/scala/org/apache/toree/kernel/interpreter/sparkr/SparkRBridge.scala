/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.toree.kernel.interpreter.sparkr

import org.apache.toree.interpreter.broker.producer.{StandardSQLContextProducer, StandardJavaSparkContextProducer, JavaSparkContextProducerLike, SQLContextProducerLike}
import org.apache.toree.interpreter.broker.{BrokerState, BrokerBridge}
import org.apache.toree.kernel.api.KernelLike
import org.apache.spark.SparkContext

/**
 * Represents constants for the SparkR bridge.
 */
object SparkRBridge {
  /** Represents the maximum amount of code that can be queued for Python. */
  val MaxQueuedCode = 500

  /** Contains the bridge used by the current R process. */
  @volatile private var _sparkRBridge: Option[SparkRBridge] = None

  /** Allows kernel to set bridge dynamically. */
  private[sparkr] def sparkRBridge_=(newSparkRBridge: SparkRBridge): Unit = {
    _sparkRBridge = Some(newSparkRBridge)
  }

  /** Clears the bridge currently hosted statically. */
  private[sparkr] def reset(): Unit = _sparkRBridge = None

  /** Must be exposed in a static location for RBackend to access. */
  def sparkRBridge: SparkRBridge = {
    assert(_sparkRBridge.nonEmpty, "SparkRBridge has not been initialized!")
    _sparkRBridge.get
  }

  /**
   * Creates a new SparkRBridge instance.
   *
   * @param brokerState The container of broker state to expose
   * @param kernel The kernel API to expose through the bridge
   *
   * @return The new SparkR bridge
   */
  def apply(
    brokerState: BrokerState,
    kernel: KernelLike
    ): SparkRBridge = {
    new SparkRBridge(
      _brokerState = brokerState,
      _kernel = kernel
    ) with StandardJavaSparkContextProducer with StandardSQLContextProducer
  }
}

/**
 * Represents the API available to SparkR to act as the bridge for data
 * between the JVM and R.
 *
 * @param _brokerState The container of broker state to expose
 * @param _kernel The kernel API to expose through the bridge
 */
class SparkRBridge private (
  private val _brokerState: BrokerState,
  private val _kernel: KernelLike
) extends BrokerBridge(_brokerState, _kernel) {
  override val brokerName: String = "SparkR"
}
