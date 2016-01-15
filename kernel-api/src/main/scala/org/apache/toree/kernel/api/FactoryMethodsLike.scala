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

package org.apache.toree.kernel.api

import java.io.{InputStream, OutputStream}

/**
 * Represents the methods available to create objects related to the kernel.
 */
trait FactoryMethodsLike {
  /**
   * Creates a new kernel output stream.
   *
   * @param streamType The type of output stream (stdout/stderr)
   * @param sendEmptyOutput If true, will send message even if output is empty
   *
   * @return The new KernelOutputStream instance
   */
  def newKernelOutputStream(
    streamType: String,
    sendEmptyOutput: Boolean
  ): OutputStream

  /**
   * Creates a new kernel input stream.
   *
   * @param prompt The text to use as a prompt
   * @param password If true, should treat input as a password field
   *
   * @return The new KernelInputStream instance
   */
  def newKernelInputStream(
    prompt: String,
    password: Boolean
  ): InputStream
}
