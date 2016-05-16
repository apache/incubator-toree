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
import org.clapper.classutil.{ClassInfo, ClassFinder}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.Try

/**
 * Represents the search utility for locating plugin classes.
 */
class PluginSearcher {
  /** Represents logger used by plugin searcher. */
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Contains all internal class information for matching plugins. */
  private lazy val internalClassInfo: Map[String, ClassInfo] =
    loadClassMap(newClassFinder())

  /** Contains all internal plugins for the system. */
  lazy val internal: Seq[ClassInfo] = findPluginClasses(internalClassInfo).toSeq

  /**
   * Searches in the provided paths (jars/zips/directories) for plugin classes.
   *
   * @param paths The paths to search through
   * @return An iterator over plugin class information
   */
  def search(paths: File*): Iterator[ClassInfo] = {
    findPluginClasses(loadClassMap(newClassFinder(paths)))
  }

  /**
   * Creates a new class finder using the JVM classpath.
   *
   * @return The new class finder
   */
  protected def newClassFinder(): ClassFinder = ClassFinder(classpath)

  /**
   * Creates a new class finder for the given paths.
   *
   * @param paths The paths within which to search for classes
   *
   * @return The new class finder
   */
  protected def newClassFinder(paths: Seq[File]): ClassFinder = ClassFinder(paths)

  /**
   * Loads all class information using the provided class finder.
   *
   * @param classFinder The class finder to use when loading class information
   * @return The map of class names to class info
   */
  private def loadClassMap(classFinder: ClassFinder): Map[String, ClassInfo] = {
    val tryStream = Try(classFinder.getClasses())
    tryStream.failed.foreach(logger.error(
      s"Failed to load class info from classpath: ${classFinder.classpath.mkString(",")}",
      _: Throwable
    ))
    val stream = tryStream.getOrElse(Stream.empty)
    ClassFinder.classInfoMap(stream.toIterator)
  }

  /**
   * Searches for classes implementing in the plugin interface, directly or
   * indirectly.
   *
   * @param classMap The map of class information to search for plugins
   * @return An iterator over plugin class information
   */
  private def findPluginClasses(classMap: Map[String, ClassInfo]): Iterator[ClassInfo] = {
    concreteSubclasses(classOf[Plugin].getName, classMap, internalClassInfo)
  }

  /**
   * Patched search that also traverses interfaces.
   *
   * @param ancestor The fully-qualified name of the class whose children to
   *                 find
   * @param classes The collection of classes to search
   * @param extraClasses Additional classes to use when traversing superclasses
   *                     and interfaces in the classes collection
   */
  private def concreteSubclasses(
    ancestor: String,
    classes: Map[String, ClassInfo],
    extraClasses: Map[String, ClassInfo]
  ): Iterator[ClassInfo] = {
    @tailrec def classMatches(
      classesToCheck: Seq[ClassInfo]
    ): Boolean = {
      if (classesToCheck.isEmpty) false
      else if (classesToCheck.exists(_.name == ancestor)) true
      else if (classesToCheck.exists(_.superClassName == ancestor)) true
      else if (classesToCheck.exists(_ implements ancestor)) true
      else {
        val superClasses = classesToCheck.map(_.superClassName)
          .flatMap(n => classes.get(n).orElse(extraClasses.get(n)))
        val interfaces = classesToCheck.flatMap(_.interfaces)
          .flatMap(i => classes.get(i).orElse(extraClasses.get(i)))
        classMatches(superClasses ++ interfaces)
      }
    }

    classes.values.toIterator
      .filter(_.isConcrete)
      .filter(c => classMatches(Seq(c)))
  }

  private def classpath = {
    new File(this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath) :: System.getProperty("java.class.path")
     .split(File.pathSeparator)
     .map(s => if (s.trim.length == 0) "." else s)
     .map(new File(_))
     .filter(_.getAbsolutePath.toLowerCase.contains("toree"))
     .toList
  }
}
