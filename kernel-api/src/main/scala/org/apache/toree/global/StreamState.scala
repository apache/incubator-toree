/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.global

import java.io.{InputStream, OutputStream, PrintStream}

/**
 * Represents the global state for input and output streams used to communicate
 * standard input and output.
 */
object StreamState {
  private val _baseInputStream = System.in
  private val _baseOutputStream = System.out
  private val _baseErrorStream = System.err

  @volatile private var _inputStream = _baseInputStream
  @volatile private var _outputStream = _baseOutputStream
  @volatile private var _errorStream = _baseErrorStream

  private def init(in: InputStream, out: OutputStream, err: OutputStream) =
    synchronized {
      System.setIn(in)
      System.setOut(new PrintStream(out))
      System.setErr(new PrintStream(err))
    }

  private def reset(): Unit = synchronized {
    System.setIn(_baseInputStream)
    System.setOut(_baseOutputStream)
    System.setErr(_baseErrorStream)
  }

  /**
   * Sets the internal streams to be used with the stream block.
   *
   * @param inputStream The input stream to map standard in
   * @param outputStream The output stream to map standard out
   * @param errorStream The output stream to map standard err
   */
  def setStreams(
    inputStream: InputStream = _inputStream,
    outputStream: OutputStream = _outputStream,
    errorStream: OutputStream = _errorStream
  ) = {
    _inputStream = inputStream
    _outputStream = new PrintStream(outputStream)
    _errorStream = new PrintStream(errorStream)
  }

  /**
   * Execute code block, mapping all input and output to the provided streams.
   */
  def withStreams[T](thunk: => T): T = {
    init(_inputStream, _outputStream, _errorStream)

    val returnValue = Console.withIn(_inputStream) {
      Console.withOut(_outputStream) {
        Console.withErr(_errorStream) {
          thunk
        }
      }
    }

    reset()

    returnValue
  }
}
