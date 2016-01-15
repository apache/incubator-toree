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

package org.apache.toree.utils

import java.io.OutputStream

case class MultiOutputStream(val outputStreams: List[OutputStream])
  extends OutputStream
{
  require(outputStreams != null)

  override def write(cbuf: Array[Byte]): Unit =
    outputStreams.foreach(outputStream => outputStream.write(cbuf))

  override def write(cbuf: Array[Byte], off: Int, len: Int): Unit =
    outputStreams.foreach(outputStream => outputStream.write(cbuf, off, len))

  override def write(b: Int): Unit =
    outputStreams.foreach(outputStream => outputStream.write(b))

  override def flush() =
    outputStreams.foreach(outputStream => outputStream.flush())

  override def close() =
    outputStreams.foreach(outputStream => outputStream.close())
}
