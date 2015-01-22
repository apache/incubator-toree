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

package com.ibm.spark.global

import java.io.{InputStream, OutputStream, PrintStream}

/**
 * Represents the global state for input and output streams used to communicate
 * standard input and output.
 */
object StreamState {
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
