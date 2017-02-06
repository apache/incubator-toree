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

import org.apache.toree.plugins.{Plugin, PluginMethodResult, PluginManager}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.language.dynamics
import scala.runtime.BoxedUnit
import scala.util.{Try, Failure, Success}

class MagicManager(private val pluginManager: PluginManager) extends Dynamic {
  protected val logger = LoggerFactory.getLogger(this.getClass.getName)
  /**
   * Checks if the provided magic is a line magic.
   *
   * @param magic The magic instance
   * @return True if the magic is an instance of a line magic
   */
  def isLineMagic(magic: Magic): Boolean =
    magic.getClass.getInterfaces.contains(classOf[LineMagic])

  /**
   * Checks if the provided magic is a cell magic.
   *
   * @param magic The magic instance
   * @return True if the magic is an instance of a cell magic
   */
  def isCellMagic(magic: Magic): Boolean =
    magic.getClass.getInterfaces.contains(classOf[CellMagic])

  /**
   * Finds a magic whose class name ends with a case insensitive name.
   *
   * @param name The name to search for
   * @return The magic
   * @throws MagicNotFoundException when no magics match name
   */
  @throws[MagicNotFoundException]
  def findMagic(name: String): Magic = {
    @tailrec def inheritsMagic(klass: Class[_]): Boolean = {
      if (klass == null) false
      else if (klass.getInterfaces.exists(classOf[Magic].isAssignableFrom)) true
      else inheritsMagic(klass.getSuperclass)
    }

    val magics = pluginManager.plugins
      .filter(p => inheritsMagic(p.getClass))
      .filter(_.simpleName.split("\\.").last.toLowerCase == name.toLowerCase)

    if (magics.size <= 0){
      logger.error(s"No magic found for $name!")
      throw new MagicNotFoundException(name)
    } else if (magics.size > 1) {
      logger.warn(s"More than one magic found for $name!")
    }

    magics.head.asInstanceOf[Magic]
  }

  @throws[MagicNotFoundException]
  def applyDynamic(name: String)(args: Any*): MagicOutput = {
    val arg = args.headOption.map(_.toString).getOrElse("")

    import org.apache.toree.plugins.Implicits._
    val result = pluginManager.fireEventFirstResult(
      name.toLowerCase(),
      "input" -> arg
    )

    result match {
      case Some(r: PluginMethodResult) => handleMagicResult(name, r.toTry)
      case None => throw new MagicNotFoundException(name)
    }
  }

  private def handleMagicResult(name: String, result: Try[Any]): MagicOutput = result match {
    case Success(magicOutput) => magicOutput match {
      case out: MagicOutput => out
      case null | _: BoxedUnit => MagicOutput()
      case cmo: Map[_, _]
        if cmo.keys.forall(_.isInstanceOf[String]) &&
          cmo.values.forall(_.isInstanceOf[String]) =>
        MagicOutput(cmo.asInstanceOf[Map[String, String]].toSeq:_*)
      case unknown =>
        val message =
          s"""Magic $name did not return proper magic output
             |type. Expected ${classOf[MagicOutput].getName}, but found
             |type of ${unknown.getClass.getName}.""".trim.stripMargin
        logger.warn(message)
        MagicOutput("text/plain" -> message)
    }
    case Failure(t) =>
      val message =  s"Magic $name failed to execute with error: \n${t.getMessage}"
      logger.warn(message, t)
      MagicOutput("text/plain" -> message)
  }
}
