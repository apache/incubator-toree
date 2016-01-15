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

package org.apache.toree.comm

import org.apache.toree.annotations.Experimental
import org.apache.toree.kernel.protocol.v5.client.ActorLoader
import org.apache.toree.kernel.protocol.v5.{KMBuilder, UUID}

/**
 * Represents a CommManager that uses a ClientCommWriter for its underlying
 * open implementation.
 *
 * @param actorLoader The actor loader to use with the ClientCommWriter
 * @param kmBuilder The KMBuilder to use with the ClientCommWriter
 * @param commRegistrar The registrar to use for callback registration
 */
@Experimental
class ClientCommManager(
  private val actorLoader: ActorLoader,
  private val kmBuilder: KMBuilder,
  private val commRegistrar: CommRegistrar
) extends CommManager(commRegistrar)
{
  /**
   * Creates a new CommWriter instance given the Comm id.
   *
   * @param commId The Comm id to use with the Comm writer
   *
   * @return The new CommWriter instance
   */
  override protected def newCommWriter(commId: UUID): CommWriter =
    new ClientCommWriter(actorLoader, kmBuilder, commId)
}
