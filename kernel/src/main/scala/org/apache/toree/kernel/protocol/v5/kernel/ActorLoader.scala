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

package org.apache.toree.kernel.protocol.v5.kernel

import akka.actor.{ActorRefFactory, ActorSelection}

/**
 * This trait defines the interface for loading actors based on some value
 * (enum, attribute, etc...). The thought is to allow external consumers
 * acquire actors through a common interface, minimizing the spread of the
 * logic about the Actors, ActorSystem, and other similar concepts.
 */
trait ActorLoader {
  /**
   * This method is meant to find an actor associated with an enum value. This
   * enum value can map to an actor associated with handling a specific kernel
   * message, a socket type, or other functionality.
   *
   * @param actorEnum The enum value used to load the actor
   *
   * @return An ActorSelection to pass messages to
   */
  def load(actorEnum: Enumeration#Value): ActorSelection
}

case class SimpleActorLoader(actorRefFactory: ActorRefFactory)
  extends ActorLoader
{
  private val userActorDirectory: String = "/user/%s"

  override def load(actorEnum: Enumeration#Value): ActorSelection = {
    actorRefFactory.actorSelection(
      userActorDirectory.format(actorEnum.toString)
    )
  }

}
