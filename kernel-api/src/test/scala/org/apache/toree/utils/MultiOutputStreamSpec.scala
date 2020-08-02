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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.mockito.Matchers._
import org.mockito.Mockito._

class MultiOutputStreamSpec
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("MultiOutputStream") {
    val listOfMockOutputStreams = List(mock[OutputStream], mock[OutputStream])
    val multiOutputStream = MultiOutputStream(listOfMockOutputStreams)

    describe("#close") {
      it("should call #close on all internal output streams") {
        multiOutputStream.close()

        listOfMockOutputStreams.foreach(mockOutputStream => verify(mockOutputStream).close())
      }
    }

    describe("#flush") {
      it("should call #flush on all internal output streams") {
        multiOutputStream.flush()

        listOfMockOutputStreams.foreach(mockOutputStream => verify(mockOutputStream).flush())
      }
    }

    describe("#write(int)") {
      it("should call #write(int) on all internal output streams") {
        multiOutputStream.write(anyInt())

        listOfMockOutputStreams.foreach(
          mockOutputStream => verify(mockOutputStream).write(anyInt()))
      }
    }
    describe("#write(byte[])") {
      it("should call #write(byte[]) on all internal output streams") {
        multiOutputStream.write(any[Array[Byte]])

        listOfMockOutputStreams.foreach(
          mockOutputStream => verify(mockOutputStream).write(any[Array[Byte]]))
      }
    }

    describe("#write(byte[], int, int)") {
      it("should call #write(byte[], int, int) on all internal output streams") {
        multiOutputStream.write(any[Array[Byte]], anyInt(), anyInt())

        listOfMockOutputStreams.foreach(
          mockOutputStream =>
            verify(mockOutputStream).write(any[Array[Byte]], anyInt(), anyInt()))
      }
    }
  }
}
