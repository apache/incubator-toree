package com.ibm.spark.kernel.api

import java.io.PrintStream

import com.ibm.spark.comm.CommManager
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.magic.MagicLoader
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class KernelSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private val BadCode = Some("abc foo bar")
  private val GoodCode = Some("val foo = 1")
  private val ErrorCode = Some("val foo = bar")
  private val ErrorMsg = "Name: error\n" +
    "Message: bad\n" +
    "StackTrace: 1"

  private var mockActorLoader: ActorLoader = _
  private var mockInterpreter: Interpreter = _
  private var mockCommManager: CommManager = _
  private var mockMagicLoader: MagicLoader = _
  private var kernel: KernelLike = _

  before {
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
      mockActorLoader, mockInterpreter, mockCommManager, mockMagicLoader
    )
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
      it("should throw an exception if the StreamInfo is not a KernelMessage") {
        implicit val streamInfo = new Object with StreamInfo
        intercept[IllegalArgumentException] {
          kernel.out
        }
      }

      it("should create a new PrintStream instance if there is StreamInfo") {
        implicit val streamInfo =
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "") with StreamInfo
        kernel.out shouldBe a [PrintStream]
      }
    }

    describe("#err") {
      it("should throw an exception if the StreamInfo is not a KernelMessage") {
        implicit val streamInfo = new Object with StreamInfo
        intercept[IllegalArgumentException] {
          kernel.err
        }
      }

      it("should create a new PrintStream instance if there is StreamInfo") {
        implicit val streamInfo =
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "") with StreamInfo

        // TODO: Access the underlying streamType field to assert stderr?
        kernel.err shouldBe a [PrintStream]
      }
    }

    describe("#stream") {
      it("should throw an exception if the StreamInfo is not a KernelMessage") {
        implicit val streamInfo = new Object with StreamInfo
        intercept[IllegalArgumentException] {
          kernel.stream
        }
      }

      it("should create a StreamMethods instance if there is a StreamInfo") {
        implicit val streamInfo =
          new KernelMessage(Nil, "", mock[Header], mock[ParentHeader],
            mock[Metadata], "") with StreamInfo
        kernel.stream shouldBe a [StreamMethods]
      }

    }
  }
}
