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

import org.apache.toree.interpreter.broker.BrokerProcessHandler

/**
 * Represents the handler for events triggered by the SparkR process.
 *
 * @param sparkRBridge The bridge to reset when the process fails or completes
 * @param restartOnFailure If true, restarts the process if it fails
 * @param restartOnCompletion If true, restarts the process if it completes
 */
class SparkRProcessHandler(
  private val sparkRBridge: SparkRBridge,
  private val restartOnFailure: Boolean,
  private val restartOnCompletion: Boolean
) extends BrokerProcessHandler(
  sparkRBridge,
  restartOnFailure,
  restartOnCompletion
) {
  override val brokerName: String = "SparkR"
}
