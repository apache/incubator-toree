package com.ibm.spark.communication.socket

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, FunSpec}
import org.zeromq.ZMQ
import org.scalatest.concurrent.Eventually._

import org.mockito.Mockito._
import org.mockito.Matchers._

class ZeroMQSocketRunnableSpec extends FunSpec with Matchers
  with OneInstancePerTest with MockitoSugar {

  private val TestAddress = "inproc://test-address"
  private val mockContext = mock[ZMQ.Context]
  private val mockSocketType = mock[SocketType]
  private val spySocket = spy(ZMQ.context(1).socket(PubSocket.`type`))

  when(mockContext.socket(anyInt())).thenReturn(spySocket)

  describe("ZeroMQSocketRunnable") {
    describe("constructor") {
      it("should throw an exception if there is no bind or connect") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(mockContext, mockSocketType, None)
        }
      }

      it("should throw an exception if there is more than one connect") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            mockContext,
            mockSocketType,
            None,
            Connect(TestAddress),
            Connect(TestAddress)
          )
        }
      }

      it("should throw an exception if there is more than one bind") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            mockContext,
            mockSocketType,
            None,
            Bind(TestAddress),
            Bind(TestAddress)
          )
        }
      }

      it("should throw an exception if there is a connect and bind") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            mockContext,
            mockSocketType,
            None,
            Connect(""),
            Bind("")
          )
        }
      }
    }

    describe("#run"){
      it("should construct a socket with the specified type") {
        val thread = new Thread(new ZeroMQSocketRunnable(
          mockContext,
          PubSocket,
          None,
          Connect(TestAddress)
        ))

        thread.start()

        eventually {
          verify(mockContext).socket(PubSocket.`type`)
        }

        thread.interrupt()
      }


      it("should set the linger option when provided") {
        val expected = 999

        val thread = new Thread(new ZeroMQSocketRunnable(
          mockContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Linger(expected)
        ))

        thread.start()

        eventually {
          val actual = spySocket.getLinger
          actual should be (expected)
        }

        thread.interrupt()
      }

      it("should set the identity option when provided") {
        val expected = "my identity".getBytes(ZMQ.CHARSET)

        val thread = new Thread(new ZeroMQSocketRunnable(
          mockContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Identity(expected)
        ))

        thread.start()

        eventually {
          val actual = spySocket.getIdentity
          actual should be (expected)
        }

        thread.interrupt()
      }

      ignore("should set non-connection options before connection options") {
        doAnswer(new Answer[Unit] {
          override def answer(invocation: InvocationOnMock): Unit = {
            verify(spySocket, never).connect(TestAddress)
          }
        }).when(spySocket).setLinger(0)

        when(spySocket.connect(TestAddress)).thenAnswer(new Answer[Unit] {
          override def answer(invocation: InvocationOnMock): Unit = {
            verify(spySocket).setLinger(0)
            verify(spySocket).subscribe(ZMQ.SUBSCRIPTION_ALL)
            verify(spySocket).setIdentity("".getBytes(ZMQ.CHARSET))
          }
        })

        new ZeroMQSocketRunnable(
          mockContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Linger(0),
          Subscribe(ZMQ.SUBSCRIPTION_ALL),
          Identity("".getBytes(ZMQ.CHARSET))
        ).run()
      }
      ignore("should set the subscribe option when provided") {
        val expected = ZMQ.SUBSCRIPTION_ALL

        new Thread(new ZeroMQSocketRunnable(
          mockContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Subscribe(expected)
        )).start()

        eventually {
          val actual = spySocket.base().getsockopt(zmq.ZMQ.ZMQ_SUBSCRIBE)
          actual should be(expected)
        }
      }
    }
    describe("#close"){
      it("should close the thread"){
          val runnable = new ZeroMQSocketRunnable(
            mockContext,
            PubSocket,
            None,
            Connect(TestAddress)
          )

          val thread = new Thread(runnable)

          thread.start()
          runnable.close()

          eventually{
            thread.isAlive should be(false)
          }
      }
    }
  }

}
