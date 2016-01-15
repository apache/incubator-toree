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

package org.apache.toree.utils

import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}
import java.util
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import scala.language.existentials

/**
 * Represents a class loader that supports delegating to multiple class loaders.
 *
 * @note Implements URLClassLoader purely to support the Guava requirement for
 *       detecting all classes.
 *
 * @param urls The URLs to use for the underlying URLClassLoader
 * @param classLoaders The class loaders to use as the underlying
 *                     implementations of this class loader
 */
class MultiClassLoader(
  private val urls: Seq[URL],
  private val classLoaders: Seq[ClassLoader]
) extends URLClassLoader(
  classLoaders.flatMap({
    case urlClassLoader: URLClassLoader => urlClassLoader.getURLs.toSeq
    case _                              => Nil
  }).distinct.toArray,
  /* Create a parent chain based on a each classloader's parent */ {
    val parents = classLoaders.flatMap(cl => Option(cl.getParent))

    // If multiple parents, set the parent to another multi class loader
    if (parents.size > 1) new MultiClassLoader(Nil, parents)

    // If a single parent, set the parent to that single parent
    else if (parents.size == 1) parents.head

    // If no parent, set to null (default if parent not provided)
    else null
  }: ClassLoader
) { self =>
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates a new multi class loader with no URLs of its own, although it may
   * still expose URLs from provided class loaders.
   *
   * @param classLoaders The class loaders to use as the underlying
   *                     implementations of this class loader
   */
  def this(classLoaders: ClassLoader*) = {
    this(Nil, classLoaders)
  }

  override protected def findClass(name: String): Class[_] = {
    @inline def tryFindClass(classLoader: ClassLoader, name: String) = {
      Try(Class.forName(name, false, classLoader))
    }

    // NOTE: Using iterator to evaluate elements one at a time
    classLoaders.toIterator
      .map(classLoader => tryFindClass(classLoader, name))
      .find(_.isSuccess)
      .map(_.get)
      .getOrElse(throw new ClassNotFoundException(name))
  }

  override protected def findResource(name: String): URL = {
    // NOTE: Using iterator to evaluate elements one at a time
    classLoaders.toIterator.map(cl => _findResource(cl, name)).find(_ != null)
      .getOrElse(super.findResource(name))
  }

  override protected def findResources(name: String): util.Enumeration[URL] = {
    val internalResources = classLoaders
      .flatMap(cl => Try(_findResources(cl, name)).toOption)
      .map(_.asScala)
      .reduce(_ ++ _)

    (
      internalResources
      ++
      Try(super.findResources(name)).map(_.asScala).getOrElse(Nil)
    ).asJavaEnumeration
  }

  private def _findResource[T <: ClassLoader](classLoader: T, name: String) = {
    _getDeclaredMethod(classLoader.getClass, "findResource", classOf[String])
      .invoke(classLoader, name).asInstanceOf[URL]
  }

  private def _findResources[T <: ClassLoader](classLoader: T, name: String) = {
    _getDeclaredMethod(classLoader.getClass, "findResources", classOf[String])
      .invoke(classLoader, name).asInstanceOf[util.Enumeration[URL]]
  }

  private def _loadClass[T <: ClassLoader](
    classLoader: T,
    name: String,
    resolve: Boolean
  ) = {
    _getDeclaredMethod(classLoader.getClass, "loadClass",
      classOf[String], classOf[Boolean]
    ).invoke(classLoader, name, resolve: java.lang.Boolean).asInstanceOf[Class[_]]
  }

  private def _getDeclaredMethod(
    klass: Class[_],
    name: String,
    classes: Class[_]*
  ): Method = {
    // Attempt to retrieve the method (public/protected/private) for the class,
    // trying the super class if the method is not available
    val potentialMethod = Try(klass.getDeclaredMethod(name, classes: _*))
      .orElse(Try(_getDeclaredMethod(klass.getSuperclass, name, classes: _*)))

    // Allow access to protected/private methods
    potentialMethod.foreach(_.setAccessible(true))

    potentialMethod match {
      case Success(method)  => method
      case Failure(error)   => throw error
    }
  }
}
