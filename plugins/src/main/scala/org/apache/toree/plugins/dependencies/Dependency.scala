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

import scala.reflect.runtime.universe.{Type, TypeTag}

/**
 * Represents a dependency.
 *
 * @param name The name of the dependency
 * @param `type` The type of the dependency
 * @param value The value of the dependency
 */
case class Dependency[T <: AnyRef](
  name: String,
  `type`: Type,
  value: T
) {
  require(name != null, "Name cannot be null!")
  require(name.nonEmpty, "Name must not be empty!")
  require(`type` != null, "Type cannot be null!")
  require(value != null, "Value cannot be null!")

  /**
   * Returns the Java class representation of this dependency's type.
   *
   * @param classLoader The class loader to use when acquiring the Java class
   * @return The Java class instance
   */
  def typeClass(classLoader: ClassLoader): Class[_] = {
    import scala.reflect.runtime.universe._
    val m = runtimeMirror(classLoader)
    m.runtimeClass(`type`.typeSymbol.asClass)
  }

  /** Represents the class for the dependency's value. */
  val valueClass = value.getClass
}

object Dependency {
  /**
   * Creates a dependency using the provided value, generating a unique name.
   * @param value The value of the dependency
   * @return The new dependency instance
   */
  def fromValue[T <: AnyRef : TypeTag](value: T) = fromValueWithName(
    java.util.UUID.randomUUID().toString,
    value
  )

  /**
   * Creates a dependency using the provided name and value.
   * @param name The name of the dependency
   * @param value The value of the dependency
   * @param typeTag The type information for the dependency's value
   * @return The new dependency instance
   */
  def fromValueWithName[T <: AnyRef : TypeTag](
    name: String,
    value: T
  )(implicit typeTag: TypeTag[T]) = Dependency(
    name = name,
    `type` = typeTag.tpe,
    value = value
  )
}