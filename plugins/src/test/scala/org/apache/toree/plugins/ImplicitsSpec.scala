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

import org.apache.toree.plugins.dependencies.Dependency
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class ImplicitsSpec extends FunSpec with Matchers with OneInstancePerTest {
  describe("Implicits") {
    describe("#$dep") {
      it("should convert values to dependencies with generated names") {
        import scala.reflect.runtime.universe._
        import org.apache.toree.plugins.Implicits._

        val value = new Object

        val d: Dependency[_] = value

        d.name should not be (empty)
        d.`type` should be (typeOf[Object])
        d.value should be (value)
      }

      it("should convert tuples of (string, value) to dependencies with the specified names") {
        import scala.reflect.runtime.universe._
        import org.apache.toree.plugins.Implicits._

        val name = "some name"
        val value = new Object

        val d: Dependency[_] = name -> value

        d.name should be (name)
        d.`type` should be (typeOf[Object])
        d.value should be (value)
      }
    }
  }
}
