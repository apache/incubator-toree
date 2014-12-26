package com.ibm.spark.kernel.api

import com.ibm.spark.comm.CommManager
import com.ibm.spark.interpreter._
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

  private var mockInterpreter: Interpreter = _
  private var mockCommManager: CommManager = _
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

    kernel = new Kernel(mockInterpreter, mockCommManager)
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
  }
}
