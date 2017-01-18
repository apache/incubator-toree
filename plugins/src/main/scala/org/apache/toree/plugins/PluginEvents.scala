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

package org.apache.toree.plugins

import scala.language.implicitConversions

sealed trait PluginEvent {
  def name: String
  
  implicit def pluginEventToString(pEvent: PluginEvent) = pEvent.name
}

/** Signaled when Spark Driver is ready. **/
case object SparkReady extends PluginEvent{
  val name = "sparkReady"
}

/** Signaled when a new OutputStream becomes available. **/
case object NewOutputStream extends PluginEvent{
  val name = "newOutputStream"
}

/** Signaled when all interpreters have been initialized. **/
case object AllInterpretersReady extends PluginEvent{
  val name = "allInterpretersReady"
}

/** Signaled before executing a block of code. **/
case object PreRunCell extends PluginEvent{
  val name = "preRunCell"
}

/** Signaled after executing a block of code. **/
case object PostRunCell extends PluginEvent{
  val name = "postRunCell"
}