package test.utils

import java.net.URL

import org.apache.toree.interpreter.{ExecuteFailure, ExecuteOutput, Interpreter}
import org.apache.toree.interpreter.Results.Result
import org.apache.toree.kernel.api.KernelLike
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

import scala.tools.nsc.interpreter.{OutputStream, InputStream}

class DummyInterpreter(kernel: KernelLike) extends Interpreter {
  /**
   * Starts the interpreter, initializing any internal state.
   * @return A reference to the interpreter
   */
  override def start(): Interpreter = ???

  /**
   * Executes body and will not print anything to the console during the execution
   * @param body The function to execute
   * @tparam T The return type of body
   * @return The return value of body
   */
  override def doQuietly[T](body: => T): T = ???

  /**
   * Stops the interpreter, removing any previous internal state.
   * @return A reference to the interpreter
   */
  override def stop(): Interpreter = ???

  /**
   * Adds external jars to the internal classpaths of the interpreter.
   * @param jars The list of jar locations
   */
  override def addJars(jars: URL*): Unit = ???

  /**
   * @return Returns a string to reference the URI of where the interpreted class files are created
   */
  override def classServerURI: String = ???

  /**
   * Returns the name of the variable created from the last execution.
   * @return Some String name if a variable was created, otherwise None
   */
  override def lastExecutionVariableName: Option[String] = ???

  /**
   * Mask the Console and System objects with our wrapper implementations
   * and dump the Console methods into the public namespace (similar to
   * the Predef approach).
   * @param in The new input stream
   * @param out The new output stream
   * @param err The new error stream
   */
  override def updatePrintStreams(in: InputStream, out: OutputStream, err: OutputStream): Unit = ???

  /**
   * Returns the class loader used by this interpreter.
   * @return The runtime class loader used by this interpreter
   */
  override def classLoader: ClassLoader = ???

  /**
   * Retrieves the contents of the variable with the provided name from the
   * interpreter.
   * @param variableName The name of the variable whose contents to read
   * @return An option containing the variable contents or None if the
   *         variable does not exist
   */
  override def read(variableName: String): Option[AnyRef] = ???

  /**
   * Interrupts the current code being interpreted.
   * @return A reference to the interpreter
   */
  override def interrupt(): Interpreter = ???

  /**
   * Binds a variable in the interpreter to a value.
   * @param variableName The name to expose the value in the interpreter
   * @param typeName The type of the variable, must be the fully qualified class name
   * @param value The value of the variable binding
   * @param modifiers Any annotation, scoping modifiers, etc on the variable
   */
  override def bind(variableName: String, typeName: String, value: Any, modifiers: List[String]): Unit = ???

  /**
   * Executes the provided code with the option to silence output.
   * @param code The code to execute
   * @param silent Whether or not to execute the code silently (no output)
   * @return The success/failure of the interpretation and the output from the
   *         execution or the failure
   */
  override def interpret(code: String, silent: Boolean): (Result, Either[ExecuteOutput, ExecuteFailure]) = ???

  /**
   * Attempts to perform code completion via the <TAB> command.
   * @param code The current cell to complete
   * @param pos The cursor position
   * @return The cursor position and list of possible completions
   */
  override def completion(code: String, pos: Int): (Int, List[String]) = ???

  /**
   * Initializes the interpreter.
   * @param kernel The kernel
   * @return The newly initialized interpreter
   */
  override def init(kernel: KernelLike): Interpreter = ???

  /**
   * Binds the SparkContext instance to the interpreter's namespace.
   *
   * @param sparkContext The SparkContext to bind
   */
  override def bindSparkContext(sparkContext: SparkContext): Unit = ???

  /**
   * Binds the SQLContext instance to the interpreter's namespace.
   *
   * @param sqlContext The SQLContext to bind
   */
  override def bindSqlContext(sqlContext: SQLContext): Unit = ???
}
