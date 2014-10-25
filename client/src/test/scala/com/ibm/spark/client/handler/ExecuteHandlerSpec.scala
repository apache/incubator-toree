package com.ibm.spark.client.handler

import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult
import com.ibm.spark.kernel.protocol.v5.{Data, Metadata}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class ExecuteHandlerSpec extends FunSpec with Matchers with MockitoSugar {

  describe("ExecuteHandler") {
    val executeResult = ExecuteResult(0, Data(), Metadata())

    describe("#resolve") {
      it("should return an execute result on success") {
        val optionReply = ExecuteHandler.resolve(executeResult)
        optionReply.get shouldNot be(None)
        optionReply.get shouldEqual executeResult
      }

      it("should return None for unhandled successes") {
        val optionReply = ExecuteHandler.resolve("foo")
        optionReply shouldEqual None
      }

      it("should rethrow an exception on failure") {
        var occurred = false
        try {
          ExecuteHandler.resolve(new RuntimeException)
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