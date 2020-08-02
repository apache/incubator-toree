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
package org.apache.toree.communication.socket

import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Socket, Context}

import scala.util.Try

class ZeroMQSocketRunnableSpec extends FunSpec with Matchers
  with MockitoSugar with Eventually with BeforeAndAfter {

  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(3, Seconds)),
    interval = scaled(Span(5, Milliseconds))
  )

  private val TestAddress = "inproc://test-address"
  private var mockSocketType: SocketType = _
  private var zmqContext: ZMQ.Context = _
  private var pubSocket: ZMQ.Socket = _

  private class TestRunnable(
    private val socket: ZMQ.Socket,
    private val context: Context,
    private val socketType: SocketType,
    private val inboundMessageCallback: Option[(Seq[Array[Byte]]) => Unit],
    private val socketOptions: SocketOption*
  ) extends ZeroMQSocketRunnable(
    context,
    socketType,
    inboundMessageCallback,
    socketOptions: _*
  ) {
    override protected def newZmqSocket(zmqContext: Context, socketType: Int): Socket = socket
  }

  before {
    mockSocketType = mock[SocketType]
    zmqContext = ZMQ.context(1)
    pubSocket = zmqContext.socket(PubSocket.`type`)
  }

  after {
    Try(zmqContext.close())
  }

  describe("ZeroMQSocketRunnable") {
    describe("constructor") {
      it("should throw an exception if there is no bind or connect") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(zmqContext, mockSocketType, None)
        }
        pubSocket.close()
      }

      it("should throw an exception if there is more than one connect") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            zmqContext,
            mockSocketType,
            None,
            Connect(TestAddress),
            Connect(TestAddress)
          )
        }
        pubSocket.close()
      }

      it("should throw an exception if there is more than one bind") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            zmqContext,
            mockSocketType,
            None,
            Bind(TestAddress),
            Bind(TestAddress)
          )
        }
        pubSocket.close()
      }

      it("should throw an exception if there is a connect and bind") {
        intercept[IllegalArgumentException] {
          new ZeroMQSocketRunnable(
            zmqContext,
            mockSocketType,
            None,
            Connect(""),
            Bind("")
          )
        }
        pubSocket.close()
      }
    }

    describe("#run"){
      it("should set the linger option when provided") {
        val expected = 999

        val runnable: TestRunnable = new TestRunnable(
          pubSocket,
          zmqContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Linger(expected)
        )
        val thread = new Thread(runnable)

        thread.start()

        eventually {
          val actual = pubSocket.getLinger
          actual should be (expected)
        }

        runnable.close()
      }

      it("should set the identity option when provided") {
        val expected = "my identity".getBytes(ZMQ.CHARSET)

        val runnable: TestRunnable = new TestRunnable(
          pubSocket,
          zmqContext,
          PubSocket,
          None,
          Connect(TestAddress),
          Identity(expected)
        )
        val thread = new Thread(runnable)

        thread.start()

        eventually {
          val actual = pubSocket.getIdentity
          actual should be (expected)
        }

        runnable.close()
      }

      it("should close the thread when closed"){
        val runnable = new TestRunnable(
          pubSocket,
          zmqContext,
          PubSocket,
          None,
          Connect(TestAddress)
        )

        val thread = new Thread(runnable)

        thread.start()

        eventually {
          runnable.isProcessing should be (true)
        }

        runnable.close()

        eventually{
          thread.isAlive should be (false)
        }
      }
    }

    describe("#isProcessing") {
      it("should be false when the runnable is closed") {
        val runnable = new TestRunnable(
          pubSocket,
          zmqContext,
          PubSocket,
          None,
          Connect(TestAddress)
        )

        val thread = new Thread(runnable)

        thread.start()

        eventually {
          runnable.isProcessing should be (true)
        }

        runnable.close()

        eventually {
          runnable.isProcessing should be (false)
        }
      }

      it("should eventually be true when the runnable is started") {
        val runnable = new TestRunnable(
          pubSocket,
          zmqContext,
          PubSocket,
          None,
          Connect(TestAddress)
        )

        val thread = new Thread(runnable)

        thread.start()

        eventually{
          runnable.isProcessing should be (true)
        }

        runnable.close()
      }
    }

    describe("#close"){
      it("should close the thread"){
          val runnable = new TestRunnable(
            pubSocket,
            zmqContext,
            PubSocket,
            None,
            Connect(TestAddress)
          )

          val thread = new Thread(runnable)

          thread.start()

          eventually {
            runnable.isProcessing should be (true)
          }

          runnable.close()

          eventually{
            thread.isAlive should be(false)
          }
      }
    }
  }

}
