package com.ibm.spark.kernel.api

import java.io.{InputStream, PrintStream}

import com.ibm.spark.comm.CommManager
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.magic.MagicLoader
import com.typesafe.config.Config
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import com.ibm.spark.global.ExecuteRequestState

class KernelSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val BadCode = Some("abc foo bar")
  private val GoodCode = Some("val foo = 1")
  private val ErrorCode = Some("val foo = bar")
  private val ErrorMsg = "Name: error\n" +
    "Message: bad\n" +
    "StackTrace: 1"

  private var mockConfig: Config = _
  private var mockActorLoader: ActorLoader = _
  private var mockInterpreter: Interpreter = _
  private var mockCommManager: CommManager = _
  private var mockMagicLoader: MagicLoader = _
  private var kernel: KernelLike = _

  before {
    mockConfig = mock[Config]
    mockInterpreter = mock[Interpreter]
    when(mockInterpreter.interpret(BadCode.get))
      .thenReturn((Results.Incomplete, null))
    when(mockInterpreter.interpret(GoodCode.get))
      .thenReturn((Results.Success, Left(new ExecuteOutput("ok"))))
    when(mockInterpreter.interpret(ErrorCode.get))
      .thenReturn((Results.Error, Right(ExecuteError("error","bad", List("1")))))

    mockCommManager = mock[CommManager]
    mockActorLoader = mock[ActorLoader]
    mockMagicLoader = mock[MagicLoader]

    kernel = new Kernel(
      mockConfig, mockActorLoader, mockInterpreter, mockCommManager,
      mockMagicLoader
    )
  }

  after {
    ExecuteRequestState.reset()
  }

  describe("Kernel") {
    describe("#eval") {
      it("should return syntax error") {
        kernel eval BadCode should be((false, "Syntax Error!"))
      }

      it("should return ok") {
        kernel eval GoodCode should be((true, "ok"))
      }

      it("should return error") {
        kernel eval ErrorCode should be((false, ErrorMsg))
      }

      it("should return error on None") {
        kernel eval None should be ((false, "Error!"))
      }
    }

    describe("#out") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.out
        }
      }

      it("should create a new PrintStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )
        kernel.out shouldBe a [PrintStream]
      }
    }

    describe("#err") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.err
        }
      }

      it("should create a new PrintStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        // TODO: Access the underlying streamType field to assert stderr?
        kernel.err shouldBe a [PrintStream]
      }
    }

    describe("#in") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.in
        }
      }

      it("should create a new InputStream instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        kernel.in shouldBe a [InputStream]
      }
    }

    describe("#stream") {
      it("should throw an exception if the ExecuteRequestState has not been set") {
        intercept[IllegalArgumentException] {
          kernel.stream
        }
      }

      it("should create a StreamMethods instance if the ExecuteRequestState has been set") {
        ExecuteRequestState.processIncomingKernelMessage(
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "")
        )

        kernel.stream shouldBe a [StreamMethods]
      }
    }
  }
}
