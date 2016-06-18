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

package org.apache.toree.kernel.protocol.v5

import org.apache.toree.kernel.BuildInfo

object SparkKernelInfo {
  /**
   * Represents the protocol version (IPython) supported by this kernel.
   */
  val protocolVersion         = "5.0"

  /**
   * Represents what the kernel implements.
   */
  val implementation          = "spark"

  /**
   * Represents the kernel version.
   */
  val implementationVersion   = BuildInfo.version

  /**
   * Represents the scala version.
   */
  val scalaVersion            = BuildInfo.scalaVersion

  /**
   * Represents the displayed name of the kernel.
   */
  val banner                  = "Apache Toree"

  /**
   * Represents the name of the user who started the kernel process.
   */
  val username                = System.getProperty("user.name")

  /**
   * Represents the unique session id used by this instance of the kernel.
   */
  val session               = java.util.UUID.randomUUID.toString
}
