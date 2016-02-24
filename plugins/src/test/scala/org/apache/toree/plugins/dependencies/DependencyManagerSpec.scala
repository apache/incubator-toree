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

import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}

class DependencyManagerSpec extends FunSpec with Matchers with OneInstancePerTest {
  private val dependencyManager = new DependencyManager

  describe("DependencyManager") {
    describe("#Empty") {
      it("should return the same dependency manager each time") {
        val expected = DependencyManager.Empty
        val actual = DependencyManager.Empty

        actual should be (expected)
      }

      it("should not add dependencies when the add method is invoked") {
        val d = DependencyManager.Empty

        d.add(new Object)
        d.add("id", new Object)
        d.add(Dependency.fromValue(new Object))

        d.toSeq should be (empty)
      }
    }

    describe("#from") {
      it("should return a new dependency manager using the dependencies") {
        val expected = Seq(
          Dependency.fromValue("value1"),
          Dependency.fromValue("value2")
        )

        val actual = DependencyManager.from(expected: _*).toSeq

        actual should contain theSameElementsAs (expected)
      }

      it("should throw an exception if two dependencies have the same name") {
        intercept[IllegalArgumentException] {
          DependencyManager.from(
            Dependency.fromValueWithName("name", "value1"),
            Dependency.fromValueWithName("name", "value2")
          )
        }
      }
    }

    describe("#merge") {
      it("should return a new dependency manager with both manager's dependencies") {
        val expected = Seq(
          Dependency.fromValue("value1"),
          Dependency.fromValue("value2"),
          Dependency.fromValue("value3"),
          Dependency.fromValue("value4")
        )

        val dm1 = DependencyManager.from(
          expected.take(expected.length / 2): _*
        )

        val dm2 = DependencyManager.from(
          expected.takeRight(expected.length / 2): _*
        )

        val actual = dm1.merge(dm2).toSeq

        actual should contain theSameElementsAs (expected)
      }

      it("should overwrite any dependency with the same name from this manager with the other") {
        val expected = Seq(
          Dependency.fromValueWithName("name", "value1"),
          Dependency.fromValue("value2"),
          Dependency.fromValue("value3"),
          Dependency.fromValue("value4")
        )

        val dm1 = DependencyManager.from(
          Dependency.fromValueWithName("name", "value5")
        )

        val dm2 = DependencyManager.from(expected: _*)

        val actual = dm1.merge(dm2).toSeq

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#toMap") {
      it("should return a map of dependency names to dependency values") {
        val expected = Map(
          "some name" -> new Object,
          "some other name" -> new Object
        )

        expected.foreach { case (k, v) => dependencyManager.add(k, v) }

        val actual = dependencyManager.toMap

        actual should be (expected)
      }
    }

    describe("#toSeq") {
      it("should return a sequence of dependency objects") {
        val expected = Seq(
          Dependency.fromValue(new Object),
          Dependency.fromValue(new Object)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.toSeq

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#add") {
      it("should generate a dependency name if not provided") {
        dependencyManager.add(new Object)

        dependencyManager.toSeq.head.name should not be (empty)
      }

      it("should use the provided name as the dependency's name") {
        val expected = "some name"

        dependencyManager.add(expected, new Object)

        val actual = dependencyManager.toSeq.head.name

        actual should be (expected)
      }

      it("should use the provided value for the dependency's value") {
        val expected = new Object

        dependencyManager.add(expected)

        val actual = dependencyManager.toSeq.head.value

        actual should be (expected)
      }

      it("should use the reflective type of the value for the dependency's type") {
        import scala.reflect.runtime.universe._

        val expected = typeOf[Object]

        dependencyManager.add(new Object)

        val actual = dependencyManager.toSeq.head.`type`

        actual should be (expected)
      }

      it("should add the provided dependency object directly") {
        val expected = Dependency.fromValue(new Object)

        dependencyManager.add(expected)

        val actual = dependencyManager.toSeq.head

        actual should be (expected)
      }

      it("should throw an exception if a dependency with the same name already exists") {
        intercept[IllegalArgumentException] {
          dependencyManager.add("id", new Object)
          dependencyManager.add("id", new Object)
        }
      }
    }

    describe("#find") {
      it("should return Some(Dependency) if found by name") {
        val expected = Some(Dependency.fromValue(new Object))

        dependencyManager.add(expected.get)

        val actual = dependencyManager.find(expected.get.name)

        actual should be (expected)
      }

      it("should return None if no dependency with a matching name exists") {
        val expected = None

        val actual = dependencyManager.find("some name")

        actual should be (expected)
      }
    }

    describe("#findByType") {
      it("should return a collection including of dependencies with the same type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[Object], new Object),
          Dependency("id2", typeOf[Object], new Object)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByType(typeOf[Object])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection including of dependencies with a sub type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new String),
          Dependency("id2", typeOf[String], new String)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByType(typeOf[Object])

        actual should contain theSameElementsAs (expected)
      }

      it("should return an empty collection if no dependency has the type") {
        import scala.reflect.runtime.universe._

        val expected = Nil

        dependencyManager.add(Dependency("id", typeOf[Object], new Object))
        dependencyManager.add(Dependency("id2", typeOf[Object], new Object))

        val actual = dependencyManager.findByType(typeOf[String])

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#findByTypeClass") {
      it("should return a collection including of dependencies with the same class for the type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[Object], new Object),
          Dependency("id2", typeOf[Object], new Object)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByTypeClass(classOf[Object])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection including of dependencies with a sub class for the type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new String),
          Dependency("id2", typeOf[String], new String)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByTypeClass(classOf[Object])

        actual should contain theSameElementsAs (expected)
      }

      it("should return an empty collection if no dependency has a matching class for its type") {
        import scala.reflect.runtime.universe._

        val expected = Nil

        dependencyManager.add(Dependency("id", typeOf[Object], new Object))
        dependencyManager.add(Dependency("id2", typeOf[Object], new Object))

        val actual = dependencyManager.findByTypeClass(classOf[String])

        actual should contain theSameElementsAs (expected)
      }

      ignore("should throw an exception if the dependency's type class is not found in the provided class' classloader") {
        import scala.reflect.runtime.universe._

        intercept[ClassNotFoundException] {
          // TODO: Find some class that is in a different classloader and
          //       create a dependency from it
          dependencyManager.add(Dependency("id", typeOf[Object], new Object))

          dependencyManager.findByTypeClass(classOf[Object])
        }
      }
    }

    describe("#findByValueClass") {
      it("should return a collection including of dependencies with the same class for the value") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[AnyVal], new AnyRef),
          Dependency("id2", typeOf[AnyVal], new AnyRef)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByValueClass(classOf[AnyRef])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection including of dependencies with a sub class for the value") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[AnyVal], new String),
          Dependency("id2", typeOf[AnyVal], new String)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.findByValueClass(classOf[AnyRef])

        actual should contain theSameElementsAs (expected)
      }

      it("should return an empty collection if no dependency has a matching class for its value") {
        import scala.reflect.runtime.universe._

        val expected = Nil

        dependencyManager.add(Dependency("id", typeOf[String], new Object))
        dependencyManager.add(Dependency("id2", typeOf[String], new Object))

        val actual = dependencyManager.findByValueClass(classOf[String])

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#remove") {
      it("should remove the dependency with the matching name") {
        val dSeq = Seq(
          Dependency.fromValue(new Object),
          Dependency.fromValue(new Object)
        )

        val dToRemove = Dependency.fromValue(new Object)
        dSeq.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        dependencyManager.remove(dToRemove.name)

        val actual = dependencyManager.toSeq

        actual should not contain (dToRemove)
      }

      it("should return Some(Dependency) representing the removed dependency") {
        val expected = Some(Dependency.fromValue(new Object))

        dependencyManager.add(expected.get)

        val actual = dependencyManager.remove(expected.get.name)

        actual should be (expected)
      }

      it("should return None if no dependency was removed") {
        val expected = None

        val actual = dependencyManager.remove("some name")

        actual should be (expected)
      }
    }

    describe("#removeByType") {
      it("should remove dependencies with the specified type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[String], new AnyRef)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByType(typeOf[String])

        actual should contain theSameElementsAs (expected)
      }

      it("should remove dependencies with a type that is a subtype of the specified type") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[String], new AnyRef)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByType(typeOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection of any removed dependencies") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[CharSequence], new AnyRef)
        )

        val all = Seq(
          Dependency("id3", typeOf[Integer], new AnyRef),
          Dependency("id4", typeOf[Boolean], new AnyRef)
        ) ++ expected

        all.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByType(typeOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#removeByTypeClass") {
      it("should remove dependencies with the specified type class") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[String], new AnyRef)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByTypeClass(classOf[String])

        actual should contain theSameElementsAs (expected)
      }

      it("should remove dependencies with a type that is a subtype of the specified type class") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[String], new AnyRef)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByTypeClass(classOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection of any removed dependencies") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[String], new AnyRef),
          Dependency("id2", typeOf[CharSequence], new AnyRef)
        )

        val all = Seq(
          Dependency("id3", typeOf[Integer], new AnyRef),
          Dependency("id4", typeOf[Boolean], new AnyRef)
        ) ++ expected

        all.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByTypeClass(classOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#removeByValueClass") {
      it("should remove dependencies with the specified value class") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[AnyRef], new String),
          Dependency("id2", typeOf[AnyRef], new String)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByValueClass(classOf[String])

        actual should contain theSameElementsAs (expected)
      }

      it("should remove dependencies with a type that is a subtype of the specified value class") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[AnyRef], new String),
          Dependency("id2", typeOf[AnyRef], new String)
        )

        expected.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByValueClass(classOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }

      it("should return a collection of any removed dependencies") {
        import scala.reflect.runtime.universe._

        val expected = Seq(
          Dependency("id", typeOf[AnyRef], new String),
          Dependency("id2", typeOf[AnyRef], new CharSequence {
            override def charAt(i: Int): Char = ???

            override def length(): Int = ???

            override def subSequence(i: Int, i1: Int): CharSequence = ???
          })
        )

        val all = Seq(
          Dependency("id3", typeOf[AnyRef], Int.box(3)),
          Dependency("id4", typeOf[AnyRef], Boolean.box(true))
        ) ++ expected

        all.foreach(dependencyManager.add(_: Dependency[_ <: AnyRef]))

        val actual = dependencyManager.removeByValueClass(classOf[CharSequence])

        actual should contain theSameElementsAs (expected)
      }
    }
  }
}
