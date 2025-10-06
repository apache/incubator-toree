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

/**
 * Represents a classloader that can load classes from within.
 *
 * @param classLoader The classloader to use for internal retrieval
 *                    (defaults to self's classloader)
 */
class InternalClassLoader(
  classLoader: ClassLoader = classOf[InternalClassLoader].getClassLoader
) extends ClassLoader(classLoader) {

  // TODO: Provides an exposed reference to the super loadClass to be stubbed
  // out in tests.
  private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] =
    super.loadClass(name, resolve)

  /**
   * Attempts to load the class using the local package of the builtin loader
   * as the base of the name if unable to load normally.
   *
   * @param name The name of the class to load
   * @param resolve If true, then resolve the class
   *
   * @return The class instance of a ClassNotFoundException
   */
  override def loadClass(name: String, resolve: Boolean): Class[_] =
    try {
      val packageName = this.getClass.getPackage.getName
      val className = name.split('.').last

      parentLoadClass(packageName + "." + className, resolve)
    } catch {
      case ex: ClassNotFoundException =>
        parentLoadClass(name, resolve)
    }
}
