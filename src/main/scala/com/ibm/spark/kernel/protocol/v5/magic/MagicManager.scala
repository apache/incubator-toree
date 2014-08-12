package com.ibm.spark.kernel.protocol.v5.magic

import akka.actor.Actor
import com.ibm.spark.magic.MagicLoader
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
    case validateMagicMessage: ValidateMagicMessage =>
      sender ! !magicRegex.findFirstIn(validateMagicMessage.toString).isEmpty

    case executeMagicMessage: ExecuteMagicMessage =>
      val matchData =
        magicRegex.findFirstMatchIn(executeMagicMessage.toString).get
      val (magicName, code) =  (matchData.group(1), matchData.after(1).toString)
      val isCell = executeMagicMessage.toString.startsWith("%%")
      var result: String = null
      if (magicLoader.hasMagic(magicName)) {
        // TODO: Offload this to another actor
        result = magicLoader.executeMagic(magicName, code, isCell)
      } else {
        // TODO: Throw an error
        result = magicLoader.executeMagic(magicName, code, isCell)
        result = s"Magic $magicName does not exist!"
      }
      sender ! result
  }

}