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

import org.apache.toree.plugins.annotations._

import scala.collection.mutable

@Priority(level = -1) class PriorityPlugin extends TestPlugin {
  @Init @Priority(level = 1)
  override def initMethod(): mutable.Seq[Any] = super.initMethod()

  @Init def initMethod2() = {}

  @Event(name = "event1") @Priority(level = 1)
  override def eventMethod(): mutable.Seq[Any] = super.eventMethod()

  @Event(name = "event1") def eventMethod2() = {}

  @Events(names = Array("event2", "event3")) @Priority(level = 1)
  override def eventsMethod(): mutable.Seq[Any] = super.eventsMethod()

  @Events(names = Array("event2", "event3"))
  def eventsMethod2() = {}

  @Destroy @Priority(level = 1)
  override def destroyMethod(): mutable.Seq[Any] = super.destroyMethod()

  @Init def destroyMethod2() = {}
}
