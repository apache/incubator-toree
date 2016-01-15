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

package org.apache.toree.interpreter.imports.printers

import java.io._

import org.apache.toree.utils.DynamicReflectionSupport

/**
 * Represents a wrapper for the scala.Console for Scala 2.10.4 implementation.
 * @param in The input stream used for standard in
 * @param out The output stream used for standard out
 * @param err The output stream used for standard error
 */
class WrapperConsole(
  val in: BufferedReader,
  val out: PrintStream,
  val err: PrintStream
) extends DynamicReflectionSupport(Class.forName("scala.Console$"), scala.Console) {
  require(in != null)
  require(out != null)
  require(err != null)

  //
  // SUPPORTED PRINT OPERATIONS
  //

  def print(obj: Any): Unit = out.print(obj)
  def printf(text: String, args: Any*): Unit =
    out.print(text.format(args: _*))
  def println(x: Any): Unit = out.println(x)
  def println(): Unit = out.println()
}
