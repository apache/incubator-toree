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

import scala.reflect.runtime.{universe => runtimeUniverse}

/**
 * Provides creation logic for magics.
 */
trait MagicCreator { this: MagicLoader =>
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
      selfType.decl(runtimeUniverse.termNames.CONSTRUCTOR).asMethod
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
