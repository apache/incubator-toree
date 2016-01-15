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

import org.apache.toree.kernel.protocol.v5
import org.apache.toree.kernel.protocol.v5.{KMBuilder, KernelMessage}
import org.apache.toree.kernel.protocol.v5.kernel.ActorLoader
import org.apache.toree.kernel.protocol.v5.stream.{KernelOutputStream, KernelInputStream}
import com.typesafe.config.Config

/**
 * Represents the methods available to stream data from the kernel to the
 * client.
 *
 * @param config The kernel configuration to use during object creation
 * @param actorLoader The actor loader to use when retrieve actors needed for
 *                    object creation
 * @param parentMessage The parent message to use when needed for object
 *                      creation
 * @param kernelMessageBuilder The builder to use when constructing kernel
 *                             messages inside objects created
 */
class FactoryMethods(
  private val config: Config,
  private val actorLoader: ActorLoader,
  private val parentMessage: KernelMessage,
  private val kernelMessageBuilder: KMBuilder
) extends FactoryMethodsLike {
  require(parentMessage != null, "Parent message cannot be null!")

  private[api] val kmBuilder = kernelMessageBuilder.withParent(parentMessage)

  /**
   * Creates a new kernel input stream.
   *
   * @param prompt The text to use as a prompt
   * @param password If true, should treat input as a password field
   *
   * @return The new KernelInputStream instance
   */
  override def newKernelInputStream(
    prompt: String = KernelInputStream.DefaultPrompt,
    password: Boolean = KernelInputStream.DefaultPassword
  ): InputStream = {
    new KernelInputStream(
      actorLoader,
      kmBuilder.withIds(parentMessage.ids),
      prompt = prompt,
      password = password
    )
  }

  /**
   * Creates a new kernel output stream.
   *
   * @param streamType The type of output stream (stdout/stderr)
   * @param sendEmptyOutput If true, will send message even if output is empty
   *
   * @return The new KernelOutputStream instance
   */
  override def newKernelOutputStream(
    streamType: String = KernelOutputStream.DefaultStreamType,
    sendEmptyOutput: Boolean = config.getBoolean("send_empty_output")
  ): OutputStream = {
    new v5.stream.KernelOutputStream(
      actorLoader,
      kmBuilder,
      org.apache.toree.global.ScheduledTaskManager.instance,
      streamType = streamType,
      sendEmptyOutput = sendEmptyOutput
    )
  }
}
