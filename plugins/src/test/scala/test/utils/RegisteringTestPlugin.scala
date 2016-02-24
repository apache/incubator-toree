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
package test.utils

import org.apache.toree.plugins.Plugin
import org.apache.toree.plugins.annotations.{Destroy, Event, Events, Init}

import RegisteringTestPlugin._

/**
 * Test plugin that registers dependencies.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class RegisteringTestPlugin extends Plugin {
  type Callback = () => Any
  private var initCallbacks = collection.mutable.Seq[Callback]()
  private var eventCallbacks = collection.mutable.Seq[Callback]()
  private var eventsCallbacks = collection.mutable.Seq[Callback]()
  private var destroyCallbacks = collection.mutable.Seq[Callback]()

  def addInitCallback(callback: Callback) = initCallbacks :+= callback
  def addEventCallback(callback: Callback) = eventCallbacks :+= callback
  def addEventsCallback(callback: Callback) = eventsCallbacks :+= callback
  def addDestroyCallback(callback: Callback) = destroyCallbacks :+= callback

  @Init def initMethod() = {
    register(InitDepName, TestPluginDependency(996))
    initCallbacks.map(_())
  }

  @Event(name = "event1") def eventMethod() = {
    register(EventDepName, TestPluginDependency(997))
    eventCallbacks.map(_())
  }

  @Events(names = Array("event2", "event3")) def eventsMethod() = {
    register(EventsDepName, TestPluginDependency(998))
    eventsCallbacks.map(_ ())
  }

  @Destroy def destroyMethod() = {
    register(DestroyDepName, TestPluginDependency(999))
    destroyCallbacks.map(_())
  }
}

object RegisteringTestPlugin {
  val DefaultEvent = "event1"
  val DefaultEvents1 = "event2"
  val DefaultEvents2 = "event3"
  val InitDepName = "init-dep"
  val EventDepName = "event-dep"
  val EventsDepName = "events-dep"
  val DestroyDepName = "destroy-dep"
}
