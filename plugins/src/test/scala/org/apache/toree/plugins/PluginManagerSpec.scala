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

import org.apache.toree.plugins.dependencies.DependencyManager
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}
import org.scalatestplus.mockito.MockitoSugar
import test.utils._

class PluginManagerSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar
{
  private val TestPluginName = "some.plugin.class.name"

  private val mockPluginClassLoader = mock[PluginClassLoader]
  private val mockPluginSearcher = mock[PluginSearcher]
  private val mockDependencyManager = mock[DependencyManager]
  private val pluginManager = new PluginManager(
    pluginClassLoader = mockPluginClassLoader,
    pluginSearcher    = mockPluginSearcher,
    dependencyManager = mockDependencyManager
  )

  describe("PluginManager") {
    describe("#isActive") {
      it("should return true if a plugin is loaded") {
        val classInfoList = Seq(
          TestClassInfo(
            name = classOf[TestPlugin].getName,
            location = new File("some/path/to/file.jar")
          )
        )

        // When returning class information
        doReturn(classInfoList.toIterator)
          .when(mockPluginSearcher).search(any[File])

        doReturn(classOf[TestPlugin])
          .when(mockPluginClassLoader).loadClass(anyString())

        // Perform the loading of plugins
        pluginManager.loadPlugins(mock[File])

        // Verify expected plugin has been loaded and is active
        pluginManager.isActive(classOf[TestPlugin].getName) should be (true)
      }

      it("should return false if a plugin is not loaded") {
        pluginManager.isActive(TestPluginName)
      }
    }

    describe("#plugins") {
      it("should return an iterator over all active plugins") {
        val classInfoList = Seq(
          TestClassInfo(
            name = classOf[TestPlugin].getName,
            location = new File("some/path/to/file.jar")
          )
        )

        // When returning class information
        doReturn(classInfoList.toIterator)
          .when(mockPluginSearcher).search(any[File])

        doReturn(classOf[TestPlugin])
          .when(mockPluginClassLoader).loadClass(anyString())

        // Perform the loading of plugins
        pluginManager.loadPlugins(mock[File])

        // Verify that we have plugins loaded
        pluginManager.plugins should have size 1
      }

      it("should be empty if no plugins are loaded") {
        pluginManager.plugins should be (empty)
      }
    }

    describe("#loadPlugin") {
      it("should return the same plugin if one exists with matching name") {
        val c = classOf[TestPlugin]
        val plugin = pluginManager.loadPlugin(c.getName, c).get

        // If name matches, doesn't try to create class
        pluginManager.loadPlugin(c.getName, null).get should be (plugin)
      }

      it("should create a new instance of the class and return it as a plugin") {
        val p = pluginManager.loadPlugin("name", classOf[TestPlugin])

        p.get shouldBe a [TestPlugin]
      }

      it("should set the internal plugin manager of the new plugin") {
        val p = pluginManager.loadPlugin("name", classOf[TestPlugin])

        p.get.internalPluginManager should be (pluginManager)
      }

      it("should add the new plugin to the list of active plugins") {
        val c = classOf[TestPlugin]
        val name = c.getName
        val plugin = pluginManager.loadPlugin(name, c).get

        pluginManager.isActive(name) should be (true)
        pluginManager.findPlugin(name).get should be (plugin)
      }

      it("should return a failure if unable to create the class instance") {
        class SomeClass(x: Int) extends Plugin
        val p = pluginManager.loadPlugin("", classOf[SomeClass])

        p.isFailure should be (true)
        // NB: Java 8 throws InstantiationException, Java 11 NoSuchMethodException
        p.failed.get shouldBe an [Exception]
      }

      it("should return an unknown plugin type failure if created class but not a plugin") {
        // NOTE: Must use global class (not nested) to find one with empty constructor
        val p = pluginManager.loadPlugin("", classOf[NotAPlugin])

        p.isFailure should be (true)
        p.failed.get shouldBe an [UnknownPluginTypeException]
      }
    }

    describe("#loadPlugins") {
      it("should throw an IllegalArgumentException if provided no paths") {
        intercept[IllegalArgumentException] {
          pluginManager.loadPlugins()
        }
      }

      it("should load nothing if the plugin searcher returns empty handed") {
        val expected = Nil

        doReturn(Iterator.empty).when(mockPluginSearcher).search(any[File])
        val actual = pluginManager.loadPlugins(mock[File])

        actual should be (expected)
      }

      it("should add paths containing plugins to the plugin class loader") {
        val classInfoList = Seq(
          TestClassInfo(
            name = "some.class",
            location = new File("some/path/to/file.jar")
          )
        )

        // When returning class information
        doReturn(classInfoList.toIterator)
          .when(mockPluginSearcher).search(any[File])

        doReturn(classOf[TestPlugin])
          .when(mockPluginClassLoader).loadClass(anyString())

        // Perform the loading of plugins
        pluginManager.loadPlugins(mock[File])

        // Should add the locations from class information
        classInfoList.map(_.location.toURI.toURL)
          .foreach(verify(mockPluginClassLoader).addURL)
      }

      it("should load the plugin classes as external plugins") {
        val classInfoList = Seq(
          TestClassInfo(
            name = classOf[TestPlugin].getName,
            location = new File("some/path/to/file.jar")
          )
        )

        // When returning class information
        doReturn(classInfoList.toIterator)
          .when(mockPluginSearcher).search(any[File])

        doReturn(classOf[TestPlugin])
          .when(mockPluginClassLoader).loadClass(anyString())

        // Perform the loading of plugins
        val plugins = pluginManager.loadPlugins(mock[File])

        // Should contain a new instance of our test plugin class
        plugins should have length 1
        plugins.head shouldBe a [TestPlugin]
      }
    }

    describe("#initializePlugins") {
      it("should send the initialize event to the specified plugins") {
        val testPlugin = new TestPlugin

        @volatile var called = false
        testPlugin.addInitCallback(() => called = true)

        pluginManager.initializePlugins(Seq(testPlugin))
        called should be (true)
      }

      it("should include any scoped dependencies in the initialized event") {
        val testPlugin = new TestPluginWithDependencies
        val dependency = TestPluginDependency(999)

        @volatile var called = false
        @volatile var d: TestPluginDependency = null
        testPlugin.addInitCallback((d2) => {
          called = true
          d = d2
        })

        val dependencyManager = new DependencyManager
        dependencyManager.add(dependency)

        doReturn(dependencyManager).when(mockDependencyManager)
          .merge(dependencyManager)

        pluginManager.initializePlugins(Seq(testPlugin), dependencyManager)
        called should be (true)
        d should be (dependency)
      }

      it("should return a collection of successes for each plugin method") {
        val testPlugin = new TestPlugin

        val results = pluginManager.initializePlugins(Seq(testPlugin))
        results should have size 1
        results.head.pluginName should be (testPlugin.name)
        results.head.isSuccess should be (true)
      }

      it("should return failures for any failed plugin method") {
        val testPlugin = new TestPlugin
        testPlugin.addInitCallback(() => throw new Throwable)

        val results = pluginManager.initializePlugins(Seq(testPlugin))
        results should have size 1
        results.head.pluginName should be (testPlugin.name)
        results.head.isFailure should be (true)
      }
    }

    describe("#findPlugin") {
      it("should return Some(plugin) if a plugin with matching name is found") {
        val c = classOf[TestPlugin]
        val p = pluginManager.loadPlugin(c.getName, c).get
        pluginManager.findPlugin(p.name) should be (Some(p))
      }

      it("should return None if no plugin with matching name is found") {
        pluginManager.findPlugin("some.class") should be (None)
      }
    }

    describe("#destroyPlugins") {
      it("should send the destroy event to the specified plugins") {
        val testPlugin = new TestPlugin

        @volatile var called = false
        testPlugin.addDestroyCallback(() => called = true)

        pluginManager.destroyPlugins(Seq(testPlugin))
        called should be (true)
      }

      it("should include any scoped dependencies in the destroy event") {
        val testPlugin = new TestPluginWithDependencies
        val dependency = TestPluginDependency(999)

        @volatile var called = false
        @volatile var d: TestPluginDependency = null
        testPlugin.addDestroyCallback((d2) => {
          called = true
          d = d2
        })

        val dependencyManager = new DependencyManager
        dependencyManager.add(dependency)

        doReturn(dependencyManager).when(mockDependencyManager)
          .merge(dependencyManager)

        pluginManager.destroyPlugins(Seq(testPlugin), dependencyManager)
        called should be (true)
        d should be (dependency)
      }

      it("should return a collection of successes for each plugin method") {
        val testPlugin = new TestPlugin

        val results = pluginManager.destroyPlugins(Seq(testPlugin))
        results should have size 1
        results.head.pluginName should be (testPlugin.name)
        results.head.isSuccess should be (true)
      }

      it("should return failures for any failed plugin method") {
        val testPlugin = new TestPlugin
        testPlugin.addDestroyCallback(() => throw new Throwable)

        val results = pluginManager.destroyPlugins(Seq(testPlugin))
        results should have size 1
        results.head.pluginName should be (testPlugin.name)
        results.head.isFailure should be (true)
      }

      it("should remove any plugin that is successfully destroyed") {
        val c = classOf[TestPlugin]

        val plugin = pluginManager.loadPlugin(c.getName, c).get
        pluginManager.plugins should have size 1

        pluginManager.destroyPlugins(Seq(plugin))
        pluginManager.plugins should be (empty)
      }

      it("should remove any plugin that fails if destroyOnFailure is true") {
        val c = classOf[TestPlugin]

        val plugin = pluginManager.loadPlugin(c.getName, c).get
        pluginManager.plugins should have size 1

        val testPlugin = plugin.asInstanceOf[TestPlugin]
        testPlugin.addDestroyCallback(() => throw new Throwable)

        pluginManager.destroyPlugins(Seq(plugin))
        pluginManager.plugins should be (empty)
      }

      it("should not remove the plugin from the list if a destroy callback fails and destroyOnFailure is false") {
        val c = classOf[TestPlugin]

        val plugin = pluginManager.loadPlugin(c.getName, c).get
        pluginManager.plugins should have size 1

        val testPlugin = plugin.asInstanceOf[TestPlugin]
        testPlugin.addDestroyCallback(() => throw new Throwable)

        pluginManager.destroyPlugins(Seq(plugin), destroyOnFailure = false)
        pluginManager.plugins should contain (testPlugin)
      }
    }

    describe("#firstEventFirstResult") {
      it("should return Some(PluginMethodResult) with first result in list if non-empty") {
        lazy val expected = Some(SuccessPluginMethodResult(
          testPlugin.eventMethodMap(TestPlugin.DefaultEvent).head,
          Seq("first")
        ))

        lazy val testPlugin = pluginManager.loadPlugin(
          classOf[TestPlugin].getName, classOf[TestPlugin]
        ).get.asInstanceOf[TestPlugin]
        testPlugin.addEventCallback(() => "first")

        lazy val testPluginWithDependencies = pluginManager.loadPlugin(
          classOf[TestPluginWithDependencies].getName,
          classOf[TestPluginWithDependencies]
        ).get.asInstanceOf[TestPluginWithDependencies]
        testPluginWithDependencies.addEventCallback(_ => "last")

        val actual = pluginManager.fireEventFirstResult(TestPlugin.DefaultEvent)

        actual should be (expected)
      }

      it("should return None if list is empty") {
        val expected = None

        val actual = pluginManager.fireEventFirstResult(TestPlugin.DefaultEvent)

        actual should be (expected)
      }
    }

    describe("#firstEventLastResult") {
      it("should return Some(Try(result)) with last result in list if non-empty") {
        lazy val expected = Some(SuccessPluginMethodResult(
          testPluginWithDependencies.eventMethodMap(TestPlugin.DefaultEvent).head,
          Seq("last")
        ))

        lazy val testPlugin = pluginManager.loadPlugin(
          classOf[TestPlugin].getName, classOf[TestPlugin]
        ).get.asInstanceOf[TestPlugin]
        testPlugin.addEventCallback(() => "first")

        lazy val testPluginWithDependencies = pluginManager.loadPlugin(
          classOf[TestPluginWithDependencies].getName,
          classOf[TestPluginWithDependencies]
        ).get.asInstanceOf[TestPluginWithDependencies]
        testPluginWithDependencies.addEventCallback(_ => "last")

        val dm = new DependencyManager
        dm.add(TestPluginDependency(999))

        doReturn(dm).when(mockDependencyManager).merge(any[DependencyManager])

        val actual = pluginManager.fireEventLastResult(
          TestPlugin.DefaultEvent, dm.toSeq: _*
        )

        actual should be (expected)
      }

      it("should return None if list is empty") {
        val expected = None

        val actual = pluginManager.fireEventLastResult(TestPlugin.DefaultEvent)

        actual should be (expected)
      }
    }

    describe("#fireEvent") {
      it("should invoke any plugin methods listening for the event") {
        val c = classOf[TestPlugin]
        val plugin = pluginManager.loadPlugin(c.getName, c).get
        val testPlugin = plugin.asInstanceOf[TestPlugin]

        @volatile var called = 0
        @volatile var calledMulti = 0
        testPlugin.addEventCallback(() => called += 1)
        testPlugin.addEventsCallback(() => calledMulti += 1)

        pluginManager.fireEvent(TestPlugin.DefaultEvent)
        pluginManager.fireEvent(TestPlugin.DefaultEvents1)
        pluginManager.fireEvent(TestPlugin.DefaultEvents2)

        called should be (1)
        calledMulti should be (2)
      }

      it("should include any scoped dependencies in the fired event") {
        val c = classOf[TestPluginWithDependencies]
        val plugin = pluginManager.loadPlugin(c.getName, c).get
        val testPlugin = plugin.asInstanceOf[TestPluginWithDependencies]
        val dependency = TestPluginDependency(999)
        val dependencyManager = new DependencyManager
        dependencyManager.add(dependency)

        @volatile var called = 0
        @volatile var calledMulti = 0
        testPlugin.addEventCallback((d) => {
          d should be (dependency)
          called += 1
        })
        testPlugin.addEventsCallback((d) => {
          d should be (dependency)
          calledMulti += 1
        })

        doReturn(dependencyManager).when(mockDependencyManager)
          .merge(dependencyManager)
        pluginManager.fireEvent(TestPlugin.DefaultEvent, dependencyManager)

        doReturn(dependencyManager).when(mockDependencyManager)
          .merge(dependencyManager)
        pluginManager.fireEvent(TestPlugin.DefaultEvents1, dependencyManager)

        doReturn(dependencyManager).when(mockDependencyManager)
          .merge(dependencyManager)
        pluginManager.fireEvent(TestPlugin.DefaultEvents2, dependencyManager)

        called should be (1)
        calledMulti should be (2)
      }

      it("should return a collection of results for invoked plugin methods") {
        val c = classOf[TestPlugin]
        val plugin = pluginManager.loadPlugin(c.getName, c).get
        val testPlugin = plugin.asInstanceOf[TestPlugin]

        testPlugin.addEventCallback(() => {})
        testPlugin.addEventsCallback(() => throw new Throwable)

        val r1 = pluginManager.fireEvent(TestPlugin.DefaultEvent)
        val r2 = pluginManager.fireEvent(TestPlugin.DefaultEvents1)

        r1 should have size 1
        r1.head.isSuccess should be (true)

        r2 should have size 1
        r2.head.isFailure should be (true)
      }

      it("should return results based on method and plugin priority") {
        val testPlugin = {
          val c = classOf[TestPlugin]
          val plugin = pluginManager.loadPlugin(c.getName, c).get
          plugin.asInstanceOf[TestPlugin]
        }

        val priorityPlugin = {
          val c = classOf[PriorityPlugin]
          val plugin = pluginManager.loadPlugin(c.getName, c).get
          plugin.asInstanceOf[PriorityPlugin]
        }

        // Order should be
        // 1. eventMethod (priority)
        // 2. eventMethod (test)
        // 3. eventMethod2 (priority)
        val r = pluginManager.fireEvent(TestPlugin.DefaultEvent)

        r.head.pluginName should be (priorityPlugin.name)
        r.head.methodName should be ("eventMethod")

        r(1).pluginName should be (testPlugin.name)
        r(1).methodName should be ("eventMethod")

        r(2).pluginName should be (priorityPlugin.name)
        r(2).methodName should be ("eventMethod2")
      }
    }
  }
}
