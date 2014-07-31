package com.ibm.spark.utils

import java.io.OutputStream

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.mockito.Matchers._
import org.mockito.Mockito._

class MultiOutputStreamSpec
  extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("MultiOutputStream") {
    val listOfMockStreams = List(mock[OutputStream], mock[OutputStream])
    val multiOutputStream = MultiOutputStream(listOfMockStreams)

    describe("#close") {
      it("should call #close on all internal streams") {
        multiOutputStream.close()

        listOfMockStreams.foreach(mockStream => verify(mockStream).close())
      }
    }

    describe("#flush") {
      it("should call #flush on all internal streams") {
        multiOutputStream.flush()

        listOfMockStreams.foreach(mockStream => verify(mockStream).flush())
      }
    }

    describe("#write(byte[])") {
      it("should call #write(byte[]) on all internal streams") {
        multiOutputStream.write(any[Array[Byte]])

        listOfMockStreams.foreach(
          mockStream => verify(mockStream).write(any[Array[Byte]]))
      }
    }

    describe("#write(byte[], int, int)") {
      it("should call #write(byte[], int, int) on all internal streams") {
        multiOutputStream.write(any[Array[Byte]], anyInt(), anyInt())

        listOfMockStreams.foreach(
          mockStream =>
            verify(mockStream).write(any[Array[Byte]], anyInt(), anyInt()))
      }
    }

    describe("#write(int)") {
      it("should call #write(int) on all internal streams") {
        multiOutputStream.write(anyInt())

        listOfMockStreams.foreach(
          mockStream => verify(mockStream).write(anyInt()))
      }
    }
  }
}
