package com.ibm.spark.kernel.protocol.v5.client.handler

import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.{Data, Metadata}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class ExecuteHandlerSpec extends FunSpec with Matchers with MockitoSugar {

  describe("ExecuteHandler") {
    val executeResult = ExecuteResult(0, Data(), Metadata())
    val executeReplyError = ExecuteReply("error",1,None,None,None,None,None)

    describe("#resolve") {
      it("should return an execute result on success") {
        val executeReply: ExecuteReply = ExecuteReply("success", 1, None, None,None,None,None)
        val optionReply: Option[Either[ExecuteReplyError, ExecuteResult]] = ExecuteHandler.resolve(executeReply, executeResult)
        optionReply.get shouldNot be(None)
        optionReply.get.isRight shouldEqual(true)
        optionReply.get match  {
          case Right(x) => x shouldEqual(executeResult)
          case Left(x) => fail("Expecting right value of ExecuteResult")
        }
      }

      it("should return None for unhandled successes") {
        val executeReply: ExecuteReply = ExecuteReply("success", 1, None, None,None,None,None)
        val optionReply = ExecuteHandler.resolve(executeReply, "foo")
        optionReply shouldEqual None
      }

      it("should rethrow an exception on failure") {
        val executeReply: ExecuteReply = ExecuteReply("error", 1, None, None,None,None,None)
        val optionReply: Option[Either[ExecuteReplyError, ExecuteResult]] = ExecuteHandler.resolve(executeReply ,new RuntimeException)
        optionReply.get shouldNot be(None)
        optionReply.get.isLeft shouldEqual(true)
        optionReply.get match  {
          case Right(x) => fail("Expecting right value of ExecuteResult")
          case Left(x) => x shouldEqual(executeReplyError)
        }
      }
    }
  }
}