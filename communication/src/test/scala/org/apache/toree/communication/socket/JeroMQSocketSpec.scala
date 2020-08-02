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

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, BeforeAndAfter, OneInstancePerTest, FunSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.zeromq.ZMsg

class JeroMQSocketSpec extends FunSpec with MockitoSugar
  with OneInstancePerTest with BeforeAndAfter with Matchers
{
  private val runnable = mock[ZeroMQSocketRunnable]
  @volatile private var running = true
  //  Mock the running of the runnable for the tests
  doAnswer(new Answer[Unit] {
    override def answer(invocation: InvocationOnMock): Unit = while (running) {
      Thread.sleep(1)
    }
  }).when(runnable).run()


  //  Mock the close of the runnable to shutdown
  doAnswer(new Answer[Unit] {
    override def answer(invocation: InvocationOnMock): Unit = running = false
  }).when(runnable).close()

  private val socket: JeroMQSocket = new JeroMQSocket(runnable)

  after {
    running = false
  }

  describe("JeroMQSocket") {
    describe("#send") {
      it("should offer a message to the runnable") {
        val message: String = "Some Message"
        val expected = ZMsg.newStringMsg(message)

        socket.send(message.getBytes)
        verify(runnable).offer(expected)
      }

      it("should thrown and AssertionError when socket is no longer alive") {
        socket.close()

        intercept[AssertionError] {
          socket.send("".getBytes)
        }
      }
    }

    describe("#close") {
      it("should close the runnable") {
        socket.close()

        verify(runnable).close()
      }

      it("should close the socket thread") {
        socket.close()

        socket.isAlive should be (false)
      }
    }

    describe("#isAlive") {
      it("should evaluate to true when the socket thread is alive") {
        socket.isAlive should be (true)
      }

      it("should evaluate to false when the socket thread is dead") {
        socket.close()

        socket.isAlive should be (false)
      }
    }
  }
}
