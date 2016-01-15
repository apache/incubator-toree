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
 * Represents a wrapper for java.lang.System.
 * @param inStream The input stream used for standard in
 * @param outStream The output stream used for standard out
 * @param errStream The output stream used for standard error
 */
class WrapperSystem(
  private val inStream: InputStream,
  private val outStream: OutputStream,
  private val errStream: OutputStream
) extends DynamicReflectionSupport(Class.forName("java.lang.System"), null){
  require(inStream != null)
  require(outStream != null)
  require(errStream != null)

  private val outPrinter = new PrintStream(outStream)
  private val errPrinter = new PrintStream(errStream)

  //
  // MASKED METHODS
  //

  def in = inStream
  def out = outPrinter
  def err = errPrinter
}
