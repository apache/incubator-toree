package com.ibm.spark.kernel.protocol.v5.relay

import java.io.OutputStream

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.ibm.spark.interpreter.{ExecuteAborted, ExecuteError}
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteResult, ExecuteRequest}
import com.ibm.spark.kernel.protocol.v5.magic.{ValidateMagicMessage, ExecuteMagicMessage}
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}

import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._

object ExecuteRequestRelaySpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestRelaySpec extends TestKit(
  ActorSystem(
    "ExecuteRequestRelayActorSystem",
    ConfigFactory.parseString(ExecuteRequestRelaySpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
  with BeforeAndAfter
{
  var mockActorLoader: ActorLoader      = _

  var magicManagerProbe: TestProbe      = _
  var interpreterActorProbe: TestProbe  = _

  before {
    mockActorLoader = mock[ActorLoader]

    magicManagerProbe = new TestProbe(system)
    val mockMagicManagerSelection =
      system.actorSelection(magicManagerProbe.ref.path.toString)
    doReturn(mockMagicManagerSelection).when(mockActorLoader)
      .load(SystemActorType.MagicManager)

    interpreterActorProbe = new TestProbe(system)
    val mockInterpreterActorSelection =
      system.actorSelection(interpreterActorProbe.ref.path.toString)
    doReturn(mockInterpreterActorSelection).when(mockActorLoader)
      .load(SystemActorType.Interpreter)
  }

  describe("ExecuteRequestRelay") {
    describe("#receive(KernelMessage)") {
      describe("when the code is magic") {
        it("should handle an error returned by the MagicManager") {
          val executeRequest =
            ExecuteRequest("%myMagic", false, true, UserExpressions(), true)
          val executeRequestRelay =
            system.actorOf(Props(classOf[ExecuteRequestRelay], mockActorLoader))

          executeRequestRelay ! ((executeRequest, mock[OutputStream]))

          // First, asks the MagicManager if the message is a magic
          // In this case, we say yes
          magicManagerProbe.expectMsgClass(classOf[ValidateMagicMessage])
          magicManagerProbe.reply(true)

          // Expected does not actually match real return of magic, which
          // is a tuple of ExecuteReply and ExecuteResult
          val expected = ExecuteError("NAME", "MESSAGE", List())
          magicManagerProbe.expectMsgClass(
            classOf[(ExecuteMagicMessage, OutputStream)]
          )
          magicManagerProbe.reply(Right(expected))

          expectMsg((
            ExecuteReplyError(1, Some(expected.name), Some(expected.value),
              Some(expected.stackTrace.map(_.toString).toList)),
            ExecuteResult(1, Data("text/plain" -> expected.toString), Metadata())
          ))
        }

        it("should ask the MagicManager for the result") {
          val executeRequest =
            ExecuteRequest("%myMagic", false, true, UserExpressions(), true)
          val executeRequestRelay =
            system.actorOf(Props(classOf[ExecuteRequestRelay], mockActorLoader))

          executeRequestRelay ! ((executeRequest, mock[OutputStream]))

          // First, asks the MagicManager if the message is a magic
          // In this case, we say yes
          magicManagerProbe.expectMsgClass(classOf[ValidateMagicMessage])
          magicManagerProbe.reply(true)

          // Expected does not actually match real return of magic, which
          // is a tuple of ExecuteReply and ExecuteResult
          val expected = "SOME VALUE"
          magicManagerProbe.expectMsgClass(
            classOf[(ExecuteMagicMessage, OutputStream)]
          )
          magicManagerProbe.reply(Left(expected))

          expectMsg((
            ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
            ExecuteResult(1, Data("text/plain" -> expected), Metadata())
          ))
        }
      }

      describe("when the code is not magic") {
        it("should handle an abort returned by the InterpreterActor") {
          val executeRequest =
            ExecuteRequest("%myMagic", false, true, UserExpressions(), true)
          val executeRequestRelay =
            system.actorOf(Props(classOf[ExecuteRequestRelay], mockActorLoader))

          executeRequestRelay ! ((executeRequest, mock[OutputStream]))

          // First, asks the MagicManager if the message is a magic
          // In this case, we say no
          magicManagerProbe.expectMsgClass(classOf[ValidateMagicMessage])
          magicManagerProbe.reply(false)

          // Expected does not actually match real return of magic, which
          // is a tuple of ExecuteReply and ExecuteResult
          val expected = new ExecuteAborted()
          interpreterActorProbe.expectMsgClass(
            classOf[(ExecuteRequest, OutputStream)]
          )
          interpreterActorProbe.reply(Right(expected))

          expectMsg((
            ExecuteReplyAbort(1),
            ExecuteResult(1, Data(), Metadata())
            ))
        }

        it("should handle an error returned by the InterpreterActor") {
          val executeRequest =
            ExecuteRequest("%myMagic", false, true, UserExpressions(), true)
          val executeRequestRelay =
            system.actorOf(Props(classOf[ExecuteRequestRelay], mockActorLoader))

          executeRequestRelay ! ((executeRequest, mock[OutputStream]))

          // First, asks the MagicManager if the message is a magic
          // In this case, we say no
          magicManagerProbe.expectMsgClass(classOf[ValidateMagicMessage])
          magicManagerProbe.reply(false)

          // Expected does not actually match real return of magic, which
          // is a tuple of ExecuteReply and ExecuteResult
          val expected = ExecuteError("NAME", "MESSAGE", List())
          interpreterActorProbe.expectMsgClass(
            classOf[(ExecuteRequest, OutputStream)]
          )
          interpreterActorProbe.reply(Right(expected))

          expectMsg((
            ExecuteReplyError(1, Some(expected.name), Some(expected.value),
              Some(expected.stackTrace.map(_.toString).toList)),
            ExecuteResult(1, Data("text/plain" -> expected.toString), Metadata())
          ))
        }

        it("should ask the ExecuteRequestHandler for the result") {
          val executeRequest =
            ExecuteRequest("notAMagic", false, true, UserExpressions(), true)
          val executeRequestRelay =
            system.actorOf(Props(classOf[ExecuteRequestRelay], mockActorLoader))

          executeRequestRelay ! ((executeRequest, mock[OutputStream]))

          // First, asks the MagicManager if the message is a magic
          // In this case, we say no
          magicManagerProbe.expectMsgClass(classOf[ValidateMagicMessage])
          magicManagerProbe.reply(false)

          // Expected does not actually match real return of interpreter, which
          // is a tuple of ExecuteReply and ExecuteResult
          val expected = "SOME OTHER VALUE"
          interpreterActorProbe.expectMsgClass(
            classOf[(ExecuteRequest, OutputStream)]
          )
          interpreterActorProbe.reply(Left(expected))

          expectMsg((
            ExecuteReplyOk(1, Some(Payloads()), Some(UserExpressions())),
            ExecuteResult(1, Data("text/plain" -> expected), Metadata())
          ))
        }
      }
    }
  }
}
