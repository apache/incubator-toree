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

import java.io.File

import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginClassLoaderSpec extends FunSpec with Matchers
  with OneInstancePerTest
{
  describe("PluginClassLoader") {
    describe("#addURL") {
      it("should add the url if not already in the loader") {
        val expected = Seq(new File("/some/file").toURI.toURL)

        val pluginClassLoader = new PluginClassLoader(Nil, null)

        // Will add for first time
        expected.foreach(pluginClassLoader.addURL)

        val actual = pluginClassLoader.getURLs

        actual should contain theSameElementsAs (expected)
      }

      it("should not add the url if already in the loader") {
        val expected = Seq(new File("/some/file").toURI.toURL)

        val pluginClassLoader = new PluginClassLoader(expected, null)

        // Will not add again
        expected.foreach(pluginClassLoader.addURL)

        val actual = pluginClassLoader.getURLs

        actual should contain theSameElementsAs (expected)
      }
    }
  }
}
