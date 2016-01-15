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

import language.dynamics
import language.existentials

import java.lang.reflect.Method

/**
 * Represents dynamic support capabilities for a class. Provides field and
 * method catchers to reflectively execute potentially-missing cases.
 * @param klass The class whose fields and methods to access
 * @param instance The specific instance whose fields and methods to access
 */
case class DynamicReflectionSupport(
  private val klass: Class[_], private val instance: Any
) extends Dynamic {

  /**
   * Handles cases of field access not found from type-checks. Attempts to
   * reflectively access field from provided class (or instance). Will first
   * check for a method signature matching field name due to Scala
   * transformation process.
   * @param name The name of the field
   * @return The content held by the field
   */
  def selectDynamic(name: String): Any = {
    val method = getMethod(name, Nil)
    method match {
      case Some(m) => invokeMethod(m, Nil)
      case _ =>
        try {
          val field = klass.getDeclaredField(name)
          field.setAccessible(true)
          field.get(instance)
        } catch {
          case ex: NoSuchFieldException =>
            throw new NoSuchFieldException(
              klass.getName + "." + name + " does not exist!"
            )
          case ex: Throwable => throw ex
        }
    }
  }

  /**
   * Handles cases of method not found from type-checks. Attempts to
   * reflectively execute the method from the provided class (or instance).
   * Will search for a method signature that has matching parameters with
   * arguments allowed to be subclasses of parameter types.
   *
   * @note Chaining in not supported. You must first cast the result of the
   *       execution before attempting to apply a method upon it.
   *
   * @param name The name of the method
   * @param args The list of arguments for the method
   * @return The result of the method's execution
   */
  def applyDynamic(name: String)(args: Any*) = {
    val method = getMethod(name, args.toList)

    method match {
      case Some(m) => invokeMethod(m, args.toList)
      case _ =>
        throw new NoSuchMethodException(
          klass.getName + "." + name +
            "(" + args.map(_.getClass.getName).mkString(",") + ")"
        )
    }
  }

  private def getMethod(name: String, args: Any*): Option[Method] = {
    val flatArgs = flatten(args)
    val potentialMethods = klass.getDeclaredMethods.filter(_.getName == name)
    val method: Option[Method] =
      potentialMethods.foldLeft[Option[Method]](None) {
        (current, m) =>
          if (current != None) current
          else if (m.getParameterTypes.size != flatArgs.size) current
          else if (!m.getParameterTypes.zipWithIndex.forall {
            case (c, i) => isCompatible(c, flatArgs(i).getClass)
          }) current
          else Some(m)
      }

    method
  }

  private def invokeMethod(method: Method, args: Any*) = {
    val flatArgs = flatten(args).map(_.asInstanceOf[AnyRef])
    method.invoke(instance, flatArgs: _*)
  }

  private def flatten(l: Seq[Any]): Seq[Any] = {
    l.flatMap {
      case newList: List[_] => flatten(newList)
      case newSeq: Seq[_] => flatten(newSeq)
      case x => List(x)
    }
  }

  private def isCompatible(klazz1: Class[_], klazz2: Class[_]): Boolean = {
    var result =
      klazz1.isAssignableFrom(klazz2) ||
      klazz1.isInstance(klazz2) ||
      klazz1.isInstanceOf[klazz2.type]

    if (!result) {
      try {
        klazz1.asInstanceOf[klazz2.type]
        result = true
      } catch {
        case _: Throwable => result = false
      }
    }

    result
  }

}
