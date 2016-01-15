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
package org.apache.toree.kernel.interpreter

import org.apache.toree.interpreter.broker.{BrokerCode, BrokerPromise}

/**
 * Contains aliases to broker types.
 */
package object sparkr {
  /**
   * Represents a promise made regarding the completion of SparkR code
   * execution.
   */
  type SparkRPromise = BrokerPromise

  /**
   * Represents a block of SparkR code to be evaluated.
   */
  type SparkRCode = BrokerCode
}
