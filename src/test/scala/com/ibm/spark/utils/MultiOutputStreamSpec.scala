package com.ibm.spark.utils

import java.io.OutputStream

import org.scalatest.mock.MockitoSugar
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
