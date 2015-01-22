package com.ibm.spark.kernel.api

import java.io.PrintStream

import com.ibm.spark.annotations.Experimental
import com.ibm.spark.comm.CommManager
import com.ibm.spark.global
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.magic.{MagicLoader, MagicExecutor}
import scala.language.dynamics

/**
 * Represents the main kernel API to be used for interaction.
 *
 * @param interpreter The interpreter to expose in this instance
 * @param comm The Comm manager to expose in this instance
 * @param actorLoader The actor loader to use for message relaying
 */
@Experimental
class Kernel (
  private val actorLoader: ActorLoader,
  val interpreter: Interpreter,
  val comm: CommManager,
  val magicLoader: MagicLoader
) extends Dynamic with KernelLike {

  val magics = new MagicExecutor(magicLoader)

  /**
   * Executes a block of code represented as a string and returns the result.
   *
   * @param code The code as an option to execute
   *
   * @return A tuple containing the result (true/false) and the output as a
   *         string
   */
  def eval(code: Option[String]): (Boolean, String) = {
    code.map(c => {
      val (success, result) = interpreter.interpret(c)
      success match {
        case Results.Success =>
          (true, result.left.getOrElse("").asInstanceOf[String])
        case Results.Error =>
          (false, result.right.getOrElse("").toString)
        case Results.Aborted =>
          (false, "Aborted!")
        case Results.Incomplete =>
          // If we get an incomplete it's most likely a syntax error, so
          // let the user know.
          (false, "Syntax Error!")
      }
    }).getOrElse((false, "Error!"))
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard out.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def out(implicit streamInfo: StreamInfo): PrintStream = {
    require(streamInfo.isInstanceOf[v5.KernelMessage],
      "The StreamInfo provided is not a KernelMessage instance!")

    val kernelMessage = streamInfo.asInstanceOf[v5.KernelMessage]
    val outputStream = new v5.stream.KernelMessageStream(
      actorLoader, v5.KMBuilder().withParent(kernelMessage),
      global.ScheduledTaskManager.instance, "stdout")

    new PrintStream(outputStream)
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard error.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def err(implicit streamInfo: StreamInfo): PrintStream = {
    require(streamInfo.isInstanceOf[v5.KernelMessage],
      "The StreamInfo provided is not a KernelMessage instance!")

    val kernelMessage = streamInfo.asInstanceOf[v5.KernelMessage]
    val outputStream = new v5.stream.KernelMessageStream(
      actorLoader, v5.KMBuilder().withParent(kernelMessage),
      global.ScheduledTaskManager.instance, "stderr")

    new PrintStream(outputStream)
  }
}