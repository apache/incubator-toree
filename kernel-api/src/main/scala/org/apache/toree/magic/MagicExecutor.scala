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

import org.apache.toree.utils.DynamicReflectionSupport

import scala.language.dynamics

class MagicExecutor(magicLoader: MagicLoader) extends Dynamic {

  val executeMethod = classOf[Magic].getDeclaredMethods.head.getName

  def applyDynamic(name: String)(args: Any*): Either[CellMagicOutput, LineMagicOutput] = {
    val className = magicLoader.magicClassName(name)
    val isCellMagic = magicLoader.hasCellMagic(className)
    val isLineMagic = magicLoader.hasLineMagic(className)

    (isCellMagic, isLineMagic) match {
      case (true, false) =>
        val result = executeMagic(className, args)
        Left(result.asInstanceOf[CellMagicOutput])
      case (false, true) =>
        executeMagic(className, args)
        Right(LineMagicOutput)
      case (_, _) =>
        Left(CellMagicOutput("text/plain" ->
          s"Magic ${className} could not be executed."))
    }
  }

  private def executeMagic(className: String, args: Seq[Any]) = {
    val inst = magicLoader.createMagicInstance(className)
    val dynamicSupport = new DynamicReflectionSupport(inst.getClass, inst)
    dynamicSupport.applyDynamic(executeMethod)(args)
  }
}
