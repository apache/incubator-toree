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
package org.apache.toree.plugins.dependencies

import org.scalatest.{FunSpec, OneInstancePerTest, Matchers}


class DependencySpec extends FunSpec with Matchers with OneInstancePerTest {
  import scala.reflect.runtime.universe._

  describe("Dependency") {
    describe("constructor") {
      it("should throw illegal argument exception if name is null") {
        intercept[IllegalArgumentException] {
          Dependency(null, typeOf[DependencySpec], new Object)
        }
      }

      it("should throw illegal argument exception if name is empty") {
        intercept[IllegalArgumentException] {
          Dependency("", typeOf[DependencySpec], new Object)
        }
      }

      it("should throw illegal argument exception if type is null") {
        intercept[IllegalArgumentException] {
          Dependency("id", null, new Object)
        }
      }

      it("should throw illegal argument exception if value is null") {
        intercept[IllegalArgumentException] {
          Dependency("id", typeOf[DependencySpec], null)
        }
      }
    }

    describe("#typeClass") {
      it("should return the class found in the class loader that matches the type") {
        val expected = this.getClass

        val d = Dependency("id", typeOf[DependencySpec], new Object)
        val actual = d.typeClass(this.getClass.getClassLoader)

        actual should be (expected)
      }

      it("should throw an exception if no matching class is found in the classloader") {
        intercept[ClassNotFoundException] {
          val d = Dependency("id", typeOf[DependencySpec], new Object)
          d.typeClass(ClassLoaderHelper.URLClassLoader(Nil, null))
        }
      }
    }

    describe("#valueClass") {
      it("should return the class directly from the dependency's value") {
        val expected = classOf[Object]

        val d = Dependency("id", typeOf[DependencySpec], new Object)
        val actual = d.valueClass

        actual should be (expected)
      }
    }

    describe("#fromValue") {
      it("should generate a unique name for the dependency") {
        val d = Dependency.fromValue(new Object)

        // TODO: Stub out UUID method to test id was generated
        d.name should not be empty
      }

      it("should use the provided value as the dependency's value") {
        val expected = new Object

        val actual = Dependency.fromValue(expected).value

        actual should be (expected)
      }

      it("should acquire the reflective type from the provided value") {
        val expected = typeOf[Object]

        val actual = Dependency.fromValue(new Object).`type`

        actual should be (expected)
      }
    }

    describe("#fromValueWithName") {
      it("should use the provided name as the name for the dependency") {
        val expected = "some dependency name"

        val actual = Dependency.fromValueWithName(expected, new Object).name

        actual should be (expected)
      }

      it("should use the provided value as the dependency's value") {
        val expected = new Object

        val actual = Dependency.fromValueWithName("id", expected).value

        actual should be (expected)
      }

      it("should acquire the reflective type from the provided value") {
        val expected = typeOf[Object]

        val actual = Dependency.fromValueWithName("id", new Object).`type`

        actual should be (expected)
      }
    }
  }
}
