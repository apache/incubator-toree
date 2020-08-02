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
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.{Matchers, FunSpec}

class ConditionalOutputStreamSpec extends FunSpec with Matchers with MockitoSugar {
  describe("ConditionalOutputStream") {
    describe("#()") {
      it("should throw an exception if the output stream is null") {
        intercept[IllegalArgumentException] {
          new ConditionalOutputStream(null, true)
        }
      }
    }

    describe("#write") {
      it("should call the underlying write if the condition is true") {
        val mockOutputStream = mock[OutputStream]
        val conditionalOutputStream =
          new ConditionalOutputStream(mockOutputStream, true)

        val expected = 101
        conditionalOutputStream.write(expected)

        verify(mockOutputStream).write(expected)
      }

      it("should call the underlying write if the condition becomes true") {
        val mockOutputStream = mock[OutputStream]
        var condition = false

        val conditionalOutputStream =
          new ConditionalOutputStream(mockOutputStream, condition)

        condition = true

        val expected = 101
        conditionalOutputStream.write(expected)

        verify(mockOutputStream).write(expected)
      }

      it("should not call the underlying write if the condition is false") {
        val mockOutputStream = mock[OutputStream]
        val conditionalOutputStream =
          new ConditionalOutputStream(mockOutputStream, false)

        val expected = 101
        conditionalOutputStream.write(expected)

        verify(mockOutputStream, never()).write(any[Byte])
      }

      it("should not call the underlying write if the condition becomes false") {
        val mockOutputStream = mock[OutputStream]
        var condition = true

        val conditionalOutputStream =
          new ConditionalOutputStream(mockOutputStream, condition)

        condition = false

        val expected = 101
        conditionalOutputStream.write(expected)

        verify(mockOutputStream, never()).write(any[Byte])
      }
    }
  }
}
