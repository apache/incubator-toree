package com.ibm.spark.interpreter

/**
 * Represents an error resulting from interpret execution.
 * @param name The name of the error
 * @param value The message provided from the error
 * @param stackTrace The stack trace as a list of strings representing lines
 *                   in the stack trace
 */
case class ExecuteError(
  name: String, value: String, stackTrace: List[String]
) {
  override def toString: String =
    "Name: " + name + "\n" +
    "Message: " + value + "\n" +
    "StackTrace: " + stackTrace.mkString("\n")
}

