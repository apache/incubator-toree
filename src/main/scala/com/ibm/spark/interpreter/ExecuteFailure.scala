package com.ibm.spark.interpreter

/**
 * Represents a generic failure in execution.
 */
sealed abstract class ExecuteFailure

/**
 * Represents an error resulting from interpret execution.
 * @param name The name of the error
 * @param value The message provided from the error
 * @param stackTrace The stack trace as a list of strings representing lines
 *                   in the stack trace
 */
case class ExecuteError(
  name: String, value: String, stackTrace: List[String]
) extends ExecuteFailure {
  override def toString: String =
    "Name: " + name + "\n" +
    "Message: " + value + "\n" +
    "StackTrace: " + stackTrace.mkString("\n")
}

// TODO: Replace with object?
/**
 * Represents an aborted execution.
 */
class ExecuteAborted extends ExecuteFailure
