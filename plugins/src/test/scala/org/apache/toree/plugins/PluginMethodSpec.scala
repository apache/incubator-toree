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

import org.apache.toree.plugins.annotations._
import org.apache.toree.plugins.dependencies.{DepClassNotFoundException, DepUnexpectedClassException, DepNameNotFoundException}
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class PluginMethodSpec extends FunSpec with Matchers with OneInstancePerTest {
  private val testThrowable = new Throwable
  private case class TestDependency(x: Int)
  private class TestPlugin extends Plugin {
    @Init def initMethod() = {}
    @Event(name = "event1") def eventMethod() = {}
    @Events(names = Array("event2", "event3")) def eventsMethod() = {}
    @Destroy def destroyMethod() = {}

    @Event(name = "event1") @Events(names = Array("event2", "event3"))
    def allEventsMethod() = {}

    @Priority(level = 999) def priorityMethod() = {}

    def dependencyMethod(testDependency: TestDependency) = testDependency

    def namedDependencyMethod(
      @DepName(name = "name") testDependency: TestDependency
    ) = testDependency

    def normalMethod() = {}

    def badMethod() = throw testThrowable
  }

  private val testPlugin = new TestPlugin

  describe("PluginMethod") {
    describe("#eventNames") {
      it("should return the event names associated with the method") {
        val expected = Seq("event1", "event2", "event3")

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("allEventsMethod")
        )

        val actual = pluginMethod.eventNames

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#isInit") {
      it("should return true if method is annotated with Init") {
        val expected = true

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("initMethod")
        )

        val actual = pluginMethod.isInit

        actual should be (expected)
      }

      it("should return false if method is not annotated with Init") {
        val expected = false

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val actual = pluginMethod.isInit

        actual should be (expected)
      }
    }

    describe("#isEvent") {
      it("should return true if method is annotated with Event") {
        val expected = true

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("eventMethod")
        )

        val actual = pluginMethod.isEvent

        actual should be (expected)
      }

      it("should return false if method is not annotated with Event") {
        val expected = false

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val actual = pluginMethod.isEvent

        actual should be (expected)
      }
    }

    describe("#isEvents") {
      it("should return true if method is annotated with Events") {
        val expected = true

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("eventsMethod")
        )

        val actual = pluginMethod.isEvents

        actual should be (expected)
      }

      it("should return false if method is not annotated with Events") {
        val expected = false

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val actual = pluginMethod.isEvents

        actual should be (expected)
      }
    }

    describe("#isDestroy") {
      it("should return true if method is annotated with Destroy") {
        val expected = true

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("destroyMethod")
        )

        val actual = pluginMethod.isDestroy

        actual should be (expected)
      }

      it("should return false if method is not annotated with Destroy") {
        val expected = false

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val actual = pluginMethod.isDestroy

        actual should be (expected)
      }
    }

    describe("#priority") {
      it("should return the priority level of the method if provided") {
        val expected = 999

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("priorityMethod")
        )

        val actual = pluginMethod.priority

        actual should be (expected)
      }

      it("should return the default priority level of the method if not provided") {
        val expected = PluginMethod.DefaultPriority

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val actual = pluginMethod.priority

        actual should be (expected)
      }
    }

    describe("#invoke") {
      it("should return a failure of DepNameNotFound if named dependency missing") {
        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod(
            "namedDependencyMethod",
            classOf[TestDependency]
          )
        )

        import org.apache.toree.plugins.Implicits._
        val result = pluginMethod.invoke(TestDependency(999))

        result.toTry.failed.get shouldBe a [DepNameNotFoundException]
      }

      it("should return a failure of DepUnexpectedClass if named dependency found with wrong class") {
        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod(
            "namedDependencyMethod",
            classOf[TestDependency]
          )
        )

        import org.apache.toree.plugins.Implicits._
        val result = pluginMethod.invoke("name" -> new AnyRef)

        result.toTry.failed.get shouldBe a [DepUnexpectedClassException]
      }

      it("should return a failure of DepClassNotFound if no dependency with class found") {
        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod(
            "dependencyMethod",
            classOf[TestDependency]
          )
        )

        val result = pluginMethod.invoke()

        result.toTry.failed.get shouldBe a [DepClassNotFoundException]
      }

      it("should return a failure of the underlying exception if an error encountered on invocation") {
        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("badMethod")
        )

        val result = pluginMethod.invoke()

        result.toTry.failed.get should be (testThrowable)
      }

      it("should return a success if invoked correctly") {
        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod("normalMethod")
        )

        val result = pluginMethod.invoke()

        result.isSuccess should be (true)
      }

      it("should be able to inject named dependencies") {
        val expected = TestDependency(999)

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod(
            "namedDependencyMethod",
            classOf[TestDependency]
          )
        )

        import org.apache.toree.plugins.Implicits._
        val result = pluginMethod.invoke(
          "name2" -> TestDependency(998),
          "name" -> expected,
          "name3" -> TestDependency(1000)
        )
        val actual = result.toTry.get

        actual should be (expected)
      }

      it("should be able to inject dependencies") {
        val expected = TestDependency(999)

        val pluginMethod = PluginMethod(
          testPlugin,
          classOf[TestPlugin].getDeclaredMethod(
            "dependencyMethod",
            classOf[TestDependency]
          )
        )

        import org.apache.toree.plugins.Implicits._
        val result = pluginMethod.invoke(
          "test",
          expected,
          Int.box(3)
        )
        val actual = result.toTry.get

        actual should be (expected)
      }
    }
  }
}
