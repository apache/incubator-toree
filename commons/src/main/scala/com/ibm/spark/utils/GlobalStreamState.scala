package com.ibm.spark.utils

import java.io.{PrintStream, OutputStream, InputStream}

/**
 * Represents the global state for input and output streams used to communicate
 * standard input and output.
 */
object GlobalStreamState {
  private val _inputStream = System.in
  private val _outputStream = System.out
  private val _errorStream = System.err

  private def init(in: InputStream, out: OutputStream, err: OutputStream) =
    synchronized {
      System.setIn(in)
      Console.setIn(in)

      System.setOut(new PrintStream(out))
      Console.setOut(out)

      System.setErr(new PrintStream(err))
      Console.setErr(err)
    }

  private def reset(): Unit = synchronized {
    System.setIn(_inputStream)
    Console.setIn(_inputStream)

    System.setOut(_outputStream)
    Console.setOut(_outputStream)

    System.setErr(_errorStream)
    Console.setErr(_errorStream)
  }

  /**
   * Execute code block, mapping all input and output to the provided streams.
   *
   * @param inputStream The input stream to map standard in
   * @param outputStream The output stream to map standard out
   * @param errorStream The output stream to map standard err
   */
  def withStreams[T](
    inputStream: InputStream = _inputStream,
    outputStream: OutputStream = _outputStream,
    errorStream: OutputStream = _errorStream
  )(thunk: => T): T = synchronized {
    init(inputStream, outputStream, errorStream)

    val returnValue = thunk

    reset()

    returnValue
  }
}
