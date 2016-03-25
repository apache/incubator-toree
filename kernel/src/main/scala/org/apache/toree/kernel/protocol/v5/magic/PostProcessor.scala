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

package org.apache.toree.kernel.protocol.v5.magic

import org.apache.toree.interpreter.{ExecuteOutput, Interpreter}
import org.apache.toree.kernel.protocol.v5.{Data, MIMEType}
import org.apache.toree.magic.{CellMagicOutput, LineMagicOutput}
import org.apache.toree.utils.LogLike

class PostProcessor(interpreter: Interpreter) extends LogLike {
  val defaultErr = "Something went wrong in postprocessor!"

  def process(codeOutput: ExecuteOutput): Data = {
    interpreter.lastExecutionVariableName.flatMap(interpreter.read) match {
      case Some(l: Left[_, _]) => matchCellMagic(codeOutput, l)
      case Some(r: Right[_, _]) => matchLineMagic(codeOutput, r)
      case _ => Data(MIMEType.PlainText -> codeOutput)
    }
  }

  protected[magic] def matchCellMagic(code: String, l: Left[_,_]) =
    l.left.getOrElse(None) match {
      // NOTE: Hack to get around erasure match issue in Scala 2.11
      case cmo: Map[_, _]
        if cmo.keys.forall(_.isInstanceOf[String]) &&
           cmo.values.forall(_.isInstanceOf[String]) =>
        cmo.asInstanceOf[CellMagicOutput]
      case _ => Data(MIMEType.PlainText -> code)
    }

  protected[magic] def matchLineMagic(code: String, r: Right[_,_]) =
    r.right.getOrElse(None) match {
      case lmo: LineMagicOutput => processLineMagic(code)
      case _ => Data(MIMEType.PlainText -> code)
    }

  protected[magic] def processLineMagic(code: String): Data = {
    val parts = code.split("\n")
    Data(MIMEType.PlainText -> parts.take(parts.size - 1).mkString("\n"))
  }
}
