package com.ibm.spark.communication.socket

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, BeforeAndAfter, OneInstancePerTest, FunSpec}
import org.scalatest.mock.MockitoSugar
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

        socket.send(message)
        verify(runnable).offer(expected)
      }

      it("should thrown and AssertionError when socket is no longer alive") {
        socket.close()

        intercept[AssertionError] {
          socket.send("")
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
