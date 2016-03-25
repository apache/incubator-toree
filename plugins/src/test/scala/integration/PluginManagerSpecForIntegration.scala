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
package integration

import org.apache.toree.plugins.{PluginManager, Plugin}
import org.apache.toree.plugins.annotations.Init
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginManagerSpecForIntegration extends FunSpec with Matchers
  with OneInstancePerTest
{
  private val pluginManager = new PluginManager

  describe("PluginManager") {
    it("should be able to load and initialize internal plugins") {
      val plugins = pluginManager.initialize()
      plugins.map(_.name) should contain allOf (
        classOf[NonCircularPlugin].getName,
        classOf[RegisterPluginA].getName,
        classOf[ConsumePluginA].getName
      )
    }

    it("should be able to initialize plugins with dependencies provided by other plugins") {
      val cpa = pluginManager.loadPlugin("", classOf[ConsumePluginA]).get
      val rpa = pluginManager.loadPlugin("", classOf[RegisterPluginA]).get

      val results = pluginManager.initializePlugins(Seq(cpa, rpa))

      results.forall(_.isSuccess) should be (true)
    }

    it("should fail when plugins have circular dependencies") {
      val cp = pluginManager.loadPlugin("", classOf[CircularPlugin]).get

      val results = pluginManager.initializePlugins(Seq(cp))

      results.forall(_.isFailure) should be (true)
    }

    it("should be able to handle non-circular dependencies within the same plugin") {
      val ncp = pluginManager.loadPlugin("", classOf[NonCircularPlugin]).get

      val results = pluginManager.initializePlugins(Seq(ncp))

      results.forall(_.isSuccess) should be (true)
    }
  }
}

private class DepA
private class DepB

private class CircularPlugin extends Plugin {
  @Init def initMethodA(depA: DepA) = register(new DepB)
  @Init def initMethodB(depB: DepB) = register(new DepA)
}

private class NonCircularPlugin extends Plugin {
  @Init def initMethodB(depB: DepB) = {}
  @Init def initMethodA(depA: DepA) = register(new DepB)
  @Init def initMethod() = register(new DepA)
}

private class RegisterPluginA extends Plugin {
  @Init def initMethod() = register(new DepA)
}

private class ConsumePluginA extends Plugin {
  @Init def initMethod(depA: DepA) = {}
}
