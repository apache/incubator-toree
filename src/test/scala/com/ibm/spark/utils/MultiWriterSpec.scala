package com.ibm.spark.utils

import java.io.Writer

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.mockito.Matchers._
import org.mockito.Mockito._

class MultiWriterSpec
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("MultiWriter") {
    val listOfMockWriters = List(mock[Writer], mock[Writer])
    val multiWriter = MultiWriter(listOfMockWriters)

    describe("#close") {
      it("should call #close on all internal writers") {
        multiWriter.close()

        listOfMockWriters.foreach(mockWriter => verify(mockWriter).close())
      }
    }

    describe("#flush") {
      it("should call #flush on all internal writers") {
        multiWriter.flush()

        listOfMockWriters.foreach(mockWriter => verify(mockWriter).flush())
      }
    }

    describe("#append(CharSequence)") {
      it("should call #append(CharSequence) on all internal writers") {
        multiWriter.append(any[CharSequence])

        listOfMockWriters.foreach(
          mockWriter => verify(mockWriter).append(any[CharSequence]))
      }
    }

    describe("#append(CharSequence, int, int)") {
      it("should call #append(CharSequence, int, int) on all internal writers") {
        multiWriter.append(any[CharSequence], anyInt(), anyInt())

        listOfMockWriters.foreach(
          mockWriter =>
            verify(mockWriter).append(any[CharSequence], anyInt(), anyInt()))
      }
    }

    describe("#append(char)") {
      it("should call #append(char) on all internal writers") {
        multiWriter.append(anyChar())

        listOfMockWriters.foreach(
          mockWriter => verify(mockWriter).append(anyChar()))
      }
    }

    describe("#write(char[])") {
      it("should call #write(char[]) on all internal writers") {
        multiWriter.write(any[Array[Char]])

        listOfMockWriters.foreach(
          mockWriter => verify(mockWriter).write(any[Array[Char]]))
      }
    }

    describe("#write(char[], int, int)") {
      it("should call #write(char[], int, int) on all internal writers") {
        multiWriter.write(any[Array[Char]], anyInt(), anyInt())

        listOfMockWriters.foreach(
          mockWriter =>
            verify(mockWriter).write(any[Array[Char]], anyInt(), anyInt()))
      }
    }

    describe("#write(int)") {
      it("should call #write(int) on all internal writers") {
        multiWriter.write(anyInt())

        listOfMockWriters.foreach(
          mockWriter => verify(mockWriter).write(anyInt()))
      }
    }
    describe("#write(String)") {
      it("should call #write(String) on all internal writers") {
        multiWriter.write(anyString())

        listOfMockWriters.foreach(
          mockWriter => verify(mockWriter).write(anyString()))
      }
    }

    describe("#write(String, int, int)") {
      it("should call #write(String, int, int) on all internal writers") {
        multiWriter.write(anyString(), anyInt(), anyInt())

        listOfMockWriters.foreach(
          mockWriter =>
            verify(mockWriter).write(anyString(), anyInt(), anyInt()))
      }
    }
  }
}
