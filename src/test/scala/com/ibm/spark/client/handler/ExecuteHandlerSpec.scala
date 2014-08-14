package com.ibm.spark.client.handler

import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.util.{Failure, Success}

class ExecuteHandlerSpec extends FunSpec with Matchers with MockitoSugar {

  describe("ExecuteHandler") {
    val reply = ExecuteReply("ok", 1, None, None, None, None, None)

    describe("#resolve") {
      it("should return an execute reply on success") {
        val optionReply = ExecuteHandler.resolve(Success(reply))
        optionReply.get shouldNot be(None)
        optionReply.get shouldEqual reply
      }

      it("should return None for unhandled successes") {
        val optionReply = ExecuteHandler.resolve(Success("foo"))
        optionReply shouldEqual None
      }

      it("should rethrow an exception on failure") {
        var occurred = false
        try {
          ExecuteHandler.resolve(Failure(new RuntimeException))
        }
        catch {
          case e: Throwable =>
            e.isInstanceOf[RuntimeException] shouldBe true
            occurred = true
        }
        occurred shouldBe true
      }
    }
  }
}