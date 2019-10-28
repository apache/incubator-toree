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

package org.apache.toree.magic

import org.scalatest.{Matchers, FunSpec}
import org.scalatestplus.mockito.MockitoSugar

class InternalClassLoaderSpec extends FunSpec with Matchers with MockitoSugar {

  abstract class MockClassLoader extends ClassLoader(null) {
    override def loadClass(name: String): Class[_] = null
  }

  describe("InternalClassLoader") {
    describe("#loadClass") {
      it("should invoke super loadClass with loader's package prepended") {
        val expected = classOf[Class[_]]
        val packageName = "org.apache.toree.magic"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            parentLoadClassCorrectlyInvoked =
              name == s"$packageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(className, true) should be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }

      it("should use loader's package instead of provided package first") {
        val expected = classOf[Class[_]]
        val forcedPackageName = "org.apache.toree.magic"
        val packageName = "some.other.package"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            parentLoadClassCorrectlyInvoked =
              name == s"$forcedPackageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(s"$packageName.$className", true) should be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }

      it("should invoke super loadClass with given package if internal missing") {
        val expected = classOf[Class[_]]
        val packageName = "some.other.package"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        var methodCalled = false
        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            if (!methodCalled) {
              methodCalled = true
              throw new ClassNotFoundException()
            }

            parentLoadClassCorrectlyInvoked =
              name == s"$packageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(s"$packageName.$className", true) should
          be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }
    }
  }
}
