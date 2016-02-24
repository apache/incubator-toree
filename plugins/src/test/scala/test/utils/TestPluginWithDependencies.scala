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

/**
 * Test plugin that provides an implementation to a plugin.
 *
 * @note Exists in global space instead of nested in test classes due to the
 *       fact that Scala creates a non-nullary constructor when a class is
 *       nested.
 */
class TestPluginWithDependencies extends Plugin {
  type Callback = (TestPluginDependency) => Any
  private var initCallbacks = collection.mutable.Seq[Callback]()
  private var eventCallbacks = collection.mutable.Seq[Callback]()
  private var eventsCallbacks = collection.mutable.Seq[Callback]()
  private var destroyCallbacks = collection.mutable.Seq[Callback]()

  def addInitCallback(callback: Callback) = initCallbacks :+= callback
  def addEventCallback(callback: Callback) = eventCallbacks :+= callback
  def addEventsCallback(callback: Callback) = eventsCallbacks :+= callback
  def addDestroyCallback(callback: Callback) = destroyCallbacks :+= callback

  @Init def initMethod(d: TestPluginDependency) = initCallbacks.map(_(d))

  @Event(name = "event1")
  def eventMethod(d: TestPluginDependency) = eventCallbacks.map(_(d))

  @Events(names = Array("event2", "event3"))
  def eventsMethod(d: TestPluginDependency) = eventsCallbacks.map(_(d))

  @Destroy def destroyMethod(d: TestPluginDependency) =
    destroyCallbacks.map(_(d))
}

case class TestPluginDependency(value: Int)

object TestPluginWithDependencies {
  val DefaultEvent = "event1"
  val DefaultEvents1 = "event2"
  val DefaultEvents2 = "event3"
}
