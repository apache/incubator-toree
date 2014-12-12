/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.magic

import java.net.{URL, URLClassLoader}

import com.google.common.reflect.ClassPath
import com.ibm.spark.magic.dependencies.DependencyMap

import scala.reflect.runtime.{universe => runtimeUniverse}
import scala.collection.JavaConversions._

class MagicLoader(
  val dependencyMap: DependencyMap = new DependencyMap(),
  urls: Array[URL] = Array(),
  parentLoader: ClassLoader = null
) extends URLClassLoader(urls, parentLoader)
{
  val magicPackage = "com.ibm.spark.magic.builtin"

  def hasMagic(name: String): Boolean = {
    val className = lowercaseClassMap(magicClassNames)
      .getOrElse(name.toLowerCase, name)
    try {
      this.loadClass(className) // Checks parent loadClass first
      true
    } catch {
      case _: Throwable => false
    }
  }

  override def loadClass(name: String, resolve: Boolean): Class[_] =
    try {
      super.loadClass(magicPackage + "." + name, resolve)
    } catch {
      case ex: ClassNotFoundException =>
        super.loadClass(name, resolve)
    }

  /**
   * Returns the class name for a case insensitive magic name query.
   * If no match is found, returns the query.
   * @param query a magic name, e.g. jAvasCRipt
   * @return the queried magic name's corresponding class, e.g. JavaScript
   */
  def magicClassName(query: String): String =
    lowercaseClassMap(magicClassNames).getOrElse(query.toLowerCase, query)

  /**
   * @return list of magic class names in magicPackage.
   */
  protected def magicClassNames : List[String] = {
    val classPath: ClassPath = ClassPath.from(this)
    val classes = classPath.getTopLevelClasses(magicPackage)
    classes.asList.map(_.getSimpleName).toList
  }

  /**
   * @param names list of class names
   * @return map of lowercase class names to class names
   */
  private def lowercaseClassMap(names: List[String]): Map[String, String] = {
    names.map(n => (n.toLowerCase, n)).toMap
  }

  protected def createMagicInstance(name: String) = {
    val magicClass = loadClass(name) // Checks parent loadClass first

    val runtimeMirror = runtimeUniverse.runtimeMirror(this)
    //val runtimeMirror = runtimeUniverse.runtimeMirror(magicClass.getClassLoader)

    val classSymbol = runtimeMirror.staticClass(magicClass.getCanonicalName)
    val classMirror = runtimeMirror.reflectClass(classSymbol)
    val selfType = classSymbol.selfType

    val classConstructorSymbol =
      selfType.declaration(runtimeUniverse.nme.CONSTRUCTOR).asMethod
    val classConstructorMethod =
      classMirror.reflectConstructor(classConstructorSymbol)

    val magicInstance = classConstructorMethod()

    // Add all of our dependencies to the new instance
    dependencyMap.internalMap.filter(selfType <:< _._1).values.foreach(
      _(magicInstance.asInstanceOf[MagicTemplate])
    )

    magicInstance
  }

  def executeMagic(name: String, code: String, isCell: Boolean): MagicOutput = {
    val magicInstance = createMagicInstance(name).asInstanceOf[MagicTemplate]

    if (isCell) {
      magicInstance.executeCell(code.split("\n"))
    } else {
      magicInstance.executeLine(code)
    }
  }
}
