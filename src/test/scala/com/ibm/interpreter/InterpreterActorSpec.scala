package com.ibm.interpreter

import java.io.{ByteArrayOutputStream, PrintStream, StringWriter}

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import akka.zeromq.ZMQMessage
import org.mockito.Matchers.{eq => mockEq}
import com.ibm.kernel.protocol.v5._
import com.ibm.kernel.protocol.v5.content.ExecuteRequest
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpecLike, Matchers}
import org.scalatest.mock._
import scala.concurrent.duration._

import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.tools.nsc.interpreter._

object InterpreterActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class InterpreterActorSpec extends TestKit(
  ActorSystem(
    "InterpreterActorSpec",
    ConfigFactory.parseString(InterpreterActorSpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
{
  describe("InterpreterActor") {
    describe("#receive") {
      it("should return an ExecuteReplyOk if the interpreter returns success") {
        val mockInterpreter = mock[ScalaInterpreter]
        doReturn(IR.Success).when(mockInterpreter).interpret(any[String])

        val interpreter =
          system.actorOf(Props(
            classOf[InterpreterActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        interpreter ! executeRequest

        expectMsgClass(10 seconds, classOf[ExecuteReplyOk])
      }

      it("should return an ExecuteReplyError if the interpreter returns error") {
        val mockInterpreter = mock[ScalaInterpreter]
        doReturn(IR.Error).when(mockInterpreter).interpret(any[String])

        val interpreter =
          system.actorOf(Props(
            classOf[InterpreterActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        interpreter ! executeRequest

        expectMsgClass(10 seconds, classOf[ExecuteReplyError])
      }

      it("should return an ExecuteReplyError if the interpreter returns incomplete") {
        val mockInterpreter = mock[ScalaInterpreter]
        doReturn(IR.Incomplete).when(mockInterpreter).interpret(any[String])

        val interpreter =
          system.actorOf(Props(
            classOf[InterpreterActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        interpreter ! executeRequest

        expectMsgClass(10 seconds, classOf[ExecuteReplyError])
      }
    }
  }
}
