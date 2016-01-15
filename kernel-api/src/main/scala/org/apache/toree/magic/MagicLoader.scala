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

package org.apache.toree.magic

import java.net.{URL, URLClassLoader}

import com.google.common.reflect.ClassPath
import org.apache.toree.magic.dependencies.DependencyMap

import scala.reflect.runtime.{universe => runtimeUniverse}
import scala.collection.JavaConversions._

class MagicLoader(
  var dependencyMap: DependencyMap = new DependencyMap(),
  urls: Array[URL] = Array(),
  parentLoader: ClassLoader = null
) extends URLClassLoader(urls, parentLoader) {
  private val magicPackage = "org.apache.toree.magic.builtin"

  /**
   * Checks whether a magic with a given name, implementing a given interface,
   * exists.
   * @param name case insensitive magic name
   * @param interface interface
   * @return true if a magic with the given name and interface exists
   */
  private def hasSpecificMagic(name: String, interface: Class[_]) : Boolean = {
    val className = magicClassName(name)
    try {
      val clazz = loadClass(className)
      clazz.getInterfaces.contains(interface)
    } catch {
      case _: Throwable => false
    }
  }

  /**
   * Checks whether a line magic exists.
   * @param name case insensitive line magic name
   * @return true if the line magic exists
   */
  def hasLineMagic(name: String): Boolean =
    hasSpecificMagic(name, classOf[LineMagic])

  /**
   * Checks whether a cell magic exists.
   * @param name case insensitive cell magic name
   * @return true if the cell magic exists
   */
  def hasCellMagic(name: String): Boolean =
    hasSpecificMagic(name, classOf[CellMagic])

  /**
   * Attempts to load a class with a given name from a package.
   * @param name the name of the class
   * @param resolve whether to resolve the class or not
   * @return the class if found
   */
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
  def magicClassName(query: String): String = {
    lowercaseClassMap(magicClassNames).getOrElse(query.toLowerCase, query)
  }

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

  def addJar(jar: URL) = addURL(jar)
  /**
   * Creates a instance of the specified magic with dependencies added.
   * @param name name of magic class
   * @return instance of the Magic corresponding to the given name
   */
  protected[magic] def createMagicInstance(name: String): Any = {
    val magicClass = loadClass(name) // Checks parent loadClass first

    val runtimeMirror = runtimeUniverse.runtimeMirror(this)
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
      _(magicInstance.asInstanceOf[Magic])
    )

    magicInstance
  }
}
