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
  def applyDynamic(name: String)(args: Any*): Either[CellMagicOutput, LineMagicOutput] = {
    val arg = args.headOption.map(_.toString).getOrElse("")

    import org.apache.toree.plugins.Implicits._
    val result = pluginManager.fireEventFirstResult(
      name.toLowerCase(),
      "input" -> arg
    )

    result match {
      case Some(r: PluginMethodResult) => handleMagicResult(name, r.toTry)
      case None => backwardsCompatibleHandleMagicResult(name, arg)
    }
  }

  private def handleMagicResult(name: String, result: Try[Any]) = result match {
     case Success(magicOutput) => magicOutput match {
        case null | _: BoxedUnit => Right(LineMagicOutput)
        case cmo: CellMagicOutput => Left(cmo)
        case unknown =>
          val message =
            s"""Magic ${name} did not return proper magic output
               |type. Expected ${classOf[CellMagicOutput].getName} or
               |${classOf[LineMagicOutput].getName}, but found type of
               |${unknown.getClass.getName}.""".trim.stripMargin
          logger.warn(message)
          Left(CellMagicOutput("text/plain" -> message))
      }
      case Failure(t) =>
        val message =  s"Magic ${name} failed to execute with error: \n${t.getMessage}"
        logger.warn(message)
        Left(CellMagicOutput("text/plain" -> message))
  }

  /**
   * This method is for backwards compatibility for magics which are not plugins
   */
  private def backwardsCompatibleHandleMagicResult(name: String, input: String) = {
    logger.warn(
      s"""Magic ${name} is not a ${classOf[Plugin].getName}.
          |Magics which are not plugins will be deprecated in a future
          |release.""".trim.stripMargin
    )
    val magic = findMagic(name)
    magic match {
      case l: LineMagic =>
        logger.trace(s"Executing LineMagic ${l.getClass.getName}")
        l.execute(input)
        Right(LineMagicOutput)
      case c: CellMagic =>
        logger.trace(s"Executing CellMagic ${c.getClass.getName}")
        val result = c.execute(input)
        Left(result.asInstanceOf[CellMagicOutput])
      case _            =>
        logger.warn(
          s"""Tried to invoke magic ${magic.getClass.getName}
              |that is neither a ${classOf[CellMagic].getName} nor
              |${classOf[LineMagic].getName}""".stripMargin
        )
        Left(CellMagicOutput("text/plain" ->
          s"Magic ${magic.getClass.getName} could not be executed."))
    }
  }
}
