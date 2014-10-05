package com.ibm.spark.utils

import java.io.OutputStream

import org.scalatest.mock.MockitoSugar
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
