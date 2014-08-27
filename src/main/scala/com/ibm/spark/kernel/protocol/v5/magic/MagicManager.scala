package com.ibm.spark.kernel.protocol.v5.magic

import java.io.OutputStream

import akka.actor.Actor
import com.ibm.spark.interpreter.{ExecuteOutput, ExecuteError}
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
    case message: ValidateMagicMessage =>
      sender ! magicRegex.findFirstIn(message.toString).nonEmpty

    case (message: ExecuteMagicMessage, outputStream: OutputStream) =>
      val matchData =
        magicRegex.findFirstMatchIn(message.toString).get
      val (magicName, code) =  (matchData.group(1), matchData.after(1).toString)
      val isCell = message.toString.startsWith("%%")

      var result: Either[ExecuteOutput, ExecuteError] = null

      if (magicLoader.hasMagic(magicName)) {
        // TODO: Offload this to another actor
        try {
          val output: ExecuteOutput =
            magicLoader.executeMagic(magicName, code, isCell)
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