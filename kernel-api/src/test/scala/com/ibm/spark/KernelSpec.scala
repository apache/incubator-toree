package com.ibm.spark

import com.ibm.spark._
import com.ibm.spark.interpreter._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

import org.scalatest.{FunSpec, Matchers}

/**
 * Created by bburns on 12/17/14.
 */
class KernelSpec extends FunSpec with Matchers with MockitoSugar {
  describe("Kernel") {
    describe("#eval") {

      val interpreter = mock[Interpreter]
      val kernel = new Kernel(interpreter)
      val badCode = Some("abc foo bar")
      val goodCode = Some("val foo = 1")
      val errorCode = Some("val foo = bar")
      val errorMsg = "Name: error\n" +
        "Message: bad\n" +
        "StackTrace: 1"

      when(interpreter.interpret(badCode.get)).thenReturn((Results.Incomplete, null))
      when(interpreter.interpret(goodCode.get)).thenReturn((Results.Success, Left(new ExecuteOutput("ok"))))
      when(interpreter.interpret(errorCode.get)).thenReturn((Results.Error, Right(ExecuteError("error","bad", List("1")))))

      it("should return syntax error") {
        kernel eval(badCode) should be((false, "Syntax Error!"))
      }

      it("should return ok") {
        kernel eval(goodCode) should be((true, "ok"))
      }

      it("should return error") {
        kernel eval(errorCode) should be((false, errorMsg))
      }

      it("should return error on None") {
        kernel eval(None) should be ((false, "Error!"))
      }
    }
  }
}
