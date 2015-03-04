package com.ibm.spark.kernel.api

import java.io.{OutputStream, InputStream, PrintStream}

import com.ibm.spark.annotations.Experimental
import com.ibm.spark.comm.CommManager
import com.ibm.spark.global
import com.ibm.spark.interpreter.Results.Result
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.protocol.v5
import com.ibm.spark.kernel.protocol.v5.KernelMessage
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.magic.MagicParser
import com.ibm.spark.kernel.protocol.v5.stream.KernelInputStream
import com.ibm.spark.magic.{MagicLoader, MagicExecutor}
import com.ibm.spark.utils.LogLike
import scala.util.DynamicVariable

import scala.reflect.runtime.universe._

import scala.language.dynamics
import com.ibm.spark.global.ExecuteRequestState

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
) extends KernelLike with LogLike {


  /**
   * Represents the current input stream used by the kernel for the specific
   * thread.
   */
  private val currentInputStream =
    new DynamicVariable[InputStream](null)
  private val currentInputKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents the current output stream used by the kernel for the specific
   * thread.
   */
  private val currentOutputStream =
    new DynamicVariable[PrintStream](null)
  private val currentOutputKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents the current error stream used by the kernel for the specific
   * thread.
   */
  private val currentErrorStream =
    new DynamicVariable[PrintStream](null)
  private val currentErrorKernelMessage =
    new DynamicVariable[KernelMessage](null)

  /**
   * Represents magics available through the kernel.
   */
  val magics = new MagicExecutor(magicLoader)

  /**
   * Represents magic parsing functionality.
   */
  val magicParser = new MagicParser(magicLoader)

  /**
   * Handles the output of interpreting code.
   * @param output the output of the interpreter
   * @return (success, message) or (failure, message)
   */
  private def handleInterpreterOutput(
    output: (Result, Either[ExecuteOutput, ExecuteFailure])
  ): (Boolean, String) = {
    val (success, result) = output
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
  }

  /**
   * Executes a block of code represented as a string and returns the result.
   *
   * @param code The code as an option to execute
   * @return A tuple containing the result (true/false) and the output as a
   *         string
   */
  def eval(code: Option[String]): (Boolean, String) = {
    code.map(c => {
      magicParser.parse(c) match {
        case Left(parsedCode) =>
          val output = interpreter.interpret(parsedCode)
          handleInterpreterOutput(output)
        case Right(errMsg) =>
          (false, errMsg)
      }
    }).getOrElse((false, "Error!"))
  }

  override def stream: StreamMethods = {
    val parentMessage = lastKernelMessage()

    new StreamMethods(actorLoader, parentMessage)
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard out.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def out: PrintStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentOutputStream, currentOutputKernelMessage, kernelMessage, {
      kernelMessage =>
        val outputStream = new v5.stream.KernelOutputStream(
          actorLoader, v5.KMBuilder().withParent(kernelMessage),
          global.ScheduledTaskManager.instance, "stdout")

        new PrintStream(outputStream)
    })
  }

  /**
   * Returns a print stream to be used for communication back to clients
   * via standard error.
   *
   * @return The print stream instance or an error if the stream info is
   *         not found
   */
  override def err: PrintStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentErrorStream, currentErrorKernelMessage, kernelMessage, { kernelMessage =>
      val outputStream = new v5.stream.KernelOutputStream(
        actorLoader, v5.KMBuilder().withParent(kernelMessage),
        global.ScheduledTaskManager.instance, "stderr")

      new PrintStream(outputStream)
    })
  }

  /**
   * Returns an input stream to be used to receive information from the client.
   *
   * @return The input stream instance or an error if the stream info is
   *         not found
   */
  override def in: InputStream = {
    val kernelMessage = lastKernelMessage()

    constructStream(currentInputStream, currentInputKernelMessage, kernelMessage, { kernelMessage =>
      new KernelInputStream(
        actorLoader,
        v5.KMBuilder()
          .withIds(kernelMessage.ids)
          .withParent(kernelMessage)
      )
    })
  }

  /**
   * Constructs or uses an existing stream.
   *
   * @param dynamicStream The DynamicVariable containing the stream to modify
   *                      or use
   * @param dynamicKernelMessage The DynamicVariable containing the KernelMessage to
   *                          check against the new KernelMessage
   * @param newKernelMessage The potentially-new KernelMessage
   * @param streamConstructionFunc The function used to create a new stream
   * @param typeTag The type information associated with the stream
   * @tparam T The stream type
   * @return The new stream or existing stream
   */
  private def constructStream[T](
    dynamicStream: DynamicVariable[T],
    dynamicKernelMessage: DynamicVariable[KernelMessage],
    newKernelMessage: KernelMessage,
    streamConstructionFunc: (KernelMessage) => T
  )(implicit typeTag: TypeTag[T]) = {
    // Update the stream being used only if the information has changed
    // or if the stream has not been initialized
    if (updateKernelMessage(dynamicKernelMessage, newKernelMessage) ||
      dynamicStream.value == null)
    {
      logger.trace("Creating new kernel " + typeTag.tpe.toString + "!")
      dynamicStream.value = streamConstructionFunc(newKernelMessage)
    }

    dynamicStream.value
  }

  /**
   * Updates the last stream info returning the status of whether or not the
   * new stream info was different than the last stream info.
   *
   * @param dynamicKernelMessage The dynamic variable containing the current
   *                          stream info
   * @param kernelMessage The new stream info
   * @return True if the new stream info is different from the last (therefore
   *         replaced), otherwise false
   */
  private def updateKernelMessage(
    dynamicKernelMessage: DynamicVariable[KernelMessage],
    kernelMessage: KernelMessage
  ): Boolean =
    if (kernelMessage != null && !kernelMessage.equals(dynamicKernelMessage.value)) {
      dynamicKernelMessage.value = kernelMessage
      true
    } else {
      false
    }

  private def lastKernelMessage() = {
    val someKernelMessage = ExecuteRequestState.lastKernelMessage
    require(someKernelMessage.nonEmpty, "No kernel message received!")
    someKernelMessage.get
  }
}
