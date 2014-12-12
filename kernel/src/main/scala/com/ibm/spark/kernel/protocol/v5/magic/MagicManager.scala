/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.magic

import java.io.OutputStream

import akka.actor.Actor
import com.ibm.spark.interpreter.ExecuteError
import com.ibm.spark.magic.{MagicLoader, MagicOutput}
import com.ibm.spark.utils.LogLike

import scala.util.parsing.combinator.RegexParsers

case class ValidateMagicMessage(message: String) {
  override def toString = message
}
case class ExecuteMagicMessage(message: String) {
  override def toString = message
}

class MagicManager(magicLoader: MagicLoader)
  extends Actor with LogLike with RegexParsers
{
  /**
   * Regular expression to match cases of %magic or %%magic as starting
   * sequence.
   */
  private val magicRegex = """^[%]{1,2}(\w+)""".r

  override def receive: Receive = {
    case message: ValidateMagicMessage =>
      sender ! magicRegex.findFirstIn(message.toString).nonEmpty

    // TODO: Support using the provided output stream, which sends messages
    //       dynamically to the KernelMessageRelay, to output magic-related
    //       print messages
    case (message: ExecuteMagicMessage, outputStream: OutputStream) =>
      val matchData =
        magicRegex.findFirstMatchIn(message.toString).get
      val (magicName, code) = (
        matchData.group(1), matchData.after(1).toString.trim
      )
      val isCell = message.toString.startsWith("%%")

      var result: Either[MagicOutput, ExecuteError] = null

      if (magicLoader.hasMagic(magicName)) {
        // TODO: Offload this to another actor
        try {

          val magicClassName = magicLoader.magicClassName(magicName)
          // Set output stream to use for this magic
          magicLoader.dependencyMap.setOutputStream(outputStream)

          val output: MagicOutput =
            magicLoader.executeMagic(magicClassName, code, isCell)
          result = Left(output)
        } catch {
          case ex: Throwable =>
            result = Right(ExecuteError(
              ex.getClass.getName,
              ex.getLocalizedMessage,
              ex.getStackTrace.map(_.toString).toList
            ))
        }
      } else {
        result = Right(ExecuteError(
          "Missing Magic", s"Magic $magicName does not exist!", List()
        ))
      }
      sender ! result
  }

}