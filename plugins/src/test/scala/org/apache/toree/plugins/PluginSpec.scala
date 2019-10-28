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

import java.lang.reflect.Method

import org.apache.toree.plugins.annotations._
import org.apache.toree.plugins.dependencies.DependencyManager
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => mockEq, _}
import scala.reflect.runtime.universe._

class PluginSpec extends FunSpec with Matchers with OneInstancePerTest with MockitoSugar {
  private val mockPluginManager = mock[PluginManager]
  private val testPlugin = {
    val plugin = new TestPlugin
    plugin.internalPluginManager_=(mockPluginManager)
    plugin
  }
  private val extendedTestPlugin = {
    val extendedPlugin = new ExtendedTestPlugin
    extendedPlugin.internalPluginManager_=(mockPluginManager)
    extendedPlugin
  }
  private val registerPlugin = new RegisterPlugin

  @Priority(level = 999) private class PriorityPlugin extends Plugin

  describe("Plugin") {
    describe("#name") {
      it("should be the name of the class implementing the plugin") {
        val expected = classOf[TestPlugin].getName

        val actual = testPlugin.name

        actual should be (expected)
      }
    }

    describe("#simpleName") {
      it("should be the simple name of the class implementing the plugin") {
        val expected = classOf[TestPlugin].getSimpleName

        val actual = testPlugin.simpleName

        actual should be (expected)
      }

      it("should be the simple name of an anonymous class implementing the plugin") {
        val anonymousPlugin = new TestPlugin {}
        val anonymousClass = anonymousPlugin.getClass
        val expected = anonymousClass.getSimpleName

        val actual = anonymousPlugin.simpleName

        actual should be(expected)
      }
    }

    describe("#priority") {
      it("should return the priority set by the plugin's annotation") {
        val expected = 999

        val actual = (new PriorityPlugin).priority

        actual should be (expected)
      }

      it("should default to zero if not set via the plugin's annotation") {
        val expected = Plugin.DefaultPriority

        val actual = (new TestPlugin).priority

        actual should be (expected)
      }
    }

    describe("#initMethods") {
      it("should return any method annotated with @Init including from ancestors") {
        val expected = Seq(
          // Inherited
          classOf[TestPlugin].getDeclaredMethod("init2"),
          classOf[TestPlugin].getDeclaredMethod("mixed2"),

          // Overridden
          classOf[ExtendedTestPlugin].getDeclaredMethod("init1"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

          // New
          classOf[ExtendedTestPlugin].getDeclaredMethod("init4"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
        ).map(PluginMethod.apply(extendedTestPlugin, _: Method))

        val actual = extendedTestPlugin.initMethods

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#destroyMethods") {
      it("should return any method annotated with @Destroy including from ancestors") {
        val expected = Seq(
          // Inherited
          classOf[TestPlugin].getDeclaredMethod("destroy2"),
          classOf[TestPlugin].getDeclaredMethod("mixed2"),

          // Overridden
          classOf[ExtendedTestPlugin].getDeclaredMethod("destroy1"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

          // New
          classOf[ExtendedTestPlugin].getDeclaredMethod("destroy4"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
        ).map(PluginMethod.apply(extendedTestPlugin, _: Method))

        val actual = extendedTestPlugin.destroyMethods

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#eventMethods") {
      it("should return any method annotated with @Event including from ancestors") {
        val expected = Seq(
          // Inherited
          classOf[TestPlugin].getDeclaredMethod("event2"),
          classOf[TestPlugin].getDeclaredMethod("mixed2"),

          // Overridden
          classOf[ExtendedTestPlugin].getDeclaredMethod("event1"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

          // New
          classOf[ExtendedTestPlugin].getDeclaredMethod("event4"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
        ).map(PluginMethod.apply(extendedTestPlugin, _: Method))

        val actual = extendedTestPlugin.eventMethods

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#eventsMethods") {
      it("should return any method annotated with @Events including from ancestors") {
        val expected = Seq(
          // Inherited
          classOf[TestPlugin].getDeclaredMethod("multiEvent2"),
          classOf[TestPlugin].getDeclaredMethod("mixed2"),

          // Overridden
          classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent1"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

          // New
          classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent4"),
          classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
        ).map(PluginMethod.apply(extendedTestPlugin, _: Method))

        val actual = extendedTestPlugin.eventsMethods

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#eventMethodMap") {
      it("should return a map of event names to their annotated methods") {
        val expected = Map(
          "event1" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("event2"),

            // Overridden
            classOf[ExtendedTestPlugin].getDeclaredMethod("event1"),
            classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent1"),

            // New
            classOf[ExtendedTestPlugin].getDeclaredMethod("event4"),
            classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent4")
          ),
          "event2" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("multiEvent2"),

            // Overridden
            classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent1"),

            // New
            classOf[ExtendedTestPlugin].getDeclaredMethod("multiEvent4")
          ),
          "event3" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("multiEvent2")
          ),
          "mixed1" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("mixed2"),

            // Overridden
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

            // New
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
          ),
          "mixed2" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("mixed2"),

            // Overridden
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

            // New
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
          ),
          "mixed3" -> Seq(
            // Inherited
            classOf[TestPlugin].getDeclaredMethod("mixed2"),

            // Overridden
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed1"),

            // New
            classOf[ExtendedTestPlugin].getDeclaredMethod("mixed4")
          )
        ).mapValues(m => m.map(PluginMethod.apply(extendedTestPlugin, _: Method)))

        val actual = extendedTestPlugin.eventMethodMap

        actual.keys should contain theSameElementsAs (expected.keys)
        actual.foreach { case (k, v) =>
          v should contain theSameElementsAs (expected(k))
        }
      }
    }

    describe("#register") {
      it("should not allow registering a dependency if the plugin manager is not set") {
        intercept[AssertionError] { registerPlugin.register(new AnyRef) }
        intercept[AssertionError] { registerPlugin.register("id", new AnyRef) }
      }

      it("should create a new name for the dependency if not specified") {
        registerPlugin.internalPluginManager_=(mockPluginManager)

        val value = new AnyRef
        val mockDependencyManager = mock[DependencyManager]
        doNothing().when(mockDependencyManager).add(anyString(), mockEq(value))(any[TypeTag[AnyRef]])
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        registerPlugin.register(value)
      }

      it("should add the dependency using the provided name") {
        registerPlugin.internalPluginManager_=(mockPluginManager)

        val name = "some name"
        val value = new AnyRef
        val mockDependencyManager = mock[DependencyManager]
        doNothing().when(mockDependencyManager).add(mockEq(name), mockEq(value))(any[TypeTag[AnyRef]])
        doReturn(mockDependencyManager).when(mockPluginManager).dependencyManager

        registerPlugin.register(name, value)
      }
    }
  }

  private class TestPlugin extends Plugin {
    @Init def init1() = {}
    @Init protected def init2() = {}
    @Init private def init3() = {}
    @Event(name = "event1") def event1() = {}
    @Event(name = "event1") protected def event2() = {}
    @Event(name = "event1") private def event3() = {}
    @Events(names = Array("event2", "event3")) def multiEvent1() = {}
    @Events(names = Array("event2", "event3")) protected def multiEvent2() = {}
    @Events(names = Array("event2", "event3")) private def multiEvent3() = {}
    @Destroy def destroy1() = {}
    @Destroy protected def destroy2() = {}
    @Destroy private def destroy3() = {}

    @Init
    @Event(name = "mixed1")
    @Events(names = Array("mixed2", "mixed3"))
    @Destroy
    def mixed1() = {}

    @Init
    @Event(name = "mixed1")
    @Events(names = Array("mixed2", "mixed3"))
    @Destroy
    protected def mixed2() = {}

    @Init
    @Event(name = "mixed1")
    @Events(names = Array("mixed2", "mixed3"))
    @Destroy
    private def mixed3() = {}
  }

  private class ExtendedTestPlugin extends TestPlugin {
    @Init override def init1() = {}
    @Event(name = "event1") override def event1() = {}
    @Events(names = Array("event1", "event2")) override def multiEvent1() = {}
    @Destroy override def destroy1() = {}
    @Init
    @Event(name = "mixed1")
    @Events(names = Array("mixed2", "mixed3"))
    @Destroy
    override def mixed1() = {}

    @Init def init4() = {}
    @Event(name = "event1") def event4() = {}
    @Events(names = Array("event1", "event2")) def multiEvent4() = {}
    @Destroy def destroy4() = {}
    @Init
    @Event(name = "mixed1")
    @Events(names = Array("mixed2", "mixed3"))
    @Destroy
    def mixed4() = {}
  }

  private class RegisterPlugin extends Plugin {
    override def register[T <: AnyRef : TypeTag](
      value: T
    ): Unit = super.register(value)
    override def register[T <: AnyRef](
      name: String,
      value: T
    )(implicit typeTag: TypeTag[T]): Unit = super.register(name, value)
  }
}
