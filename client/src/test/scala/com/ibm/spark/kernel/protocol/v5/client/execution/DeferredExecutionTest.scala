/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.content.{StreamContent, ExecuteResult}
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5._
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Promise
import scala.util.{Try, Failure, Success}

object DeferredExecutionTest {
  val executeReplyError  = ExecuteReplyError(1, None, None, None)
  val executeReplyOk     = ExecuteReplyOk(1, None, None)
  val executeReplyResult = ExecuteResult(1, Data(), Metadata())

  def mockSendingKernelMessages(deferredExecution: DeferredExecution,
                                executeReply: ExecuteReply,
                                executeResult: ExecuteResult): Unit = {
    //  Mock behaviour of the kernel sending messages to us
    deferredExecution.resolveResult(executeResult)
    deferredExecution.resolveReply(executeReply)
  }
  
  def processExecuteResult(executeResult: ExecuteResult)
          (implicit promise: Promise[Try[ExecuteResultPromise]]): Unit = {
    promise.success(Success(new ExecuteResultPromise))
  }

  def processExecuteReply(executeReplyError: ExecuteReplyError)
          (implicit promise: Promise[Try[ExecuteReplyPromise]]): Unit = {
    promise.success(Success(new ExecuteReplyPromise))
  }
}

class ExecuteResultPromise {}
class ExecuteReplyPromise {}

class DeferredExecutionTest extends FunSpec with ScalaFutures with Matchers {
  import DeferredExecutionTest._
  describe("DeferredExecution") {
    describe("onResult( callback )"){
      it("should run all onResult callbacks when ExecuteResult and " +
         "successful ExecuteReply are returned") {
        implicit val executeResultPromise: Promise[Try[ExecuteResultPromise]] = Promise()
        val deferredExecution: DeferredExecution = DeferredExecution()
          .onResult(processExecuteResult)

        mockSendingKernelMessages(deferredExecution, executeReplyOk, executeReplyResult)

        whenReady(executeResultPromise.future) {
          case Success(v) => assert(true)
          case Failure(exception: Throwable) =>
            fail("Promise resolved with failure when processing " +
                 "execute result.", exception)
          case unknownValue=>
            fail(s"Promised resolved with unknown value: ${unknownValue}")
        }
      }
      it("should run all onResultCallbacks registered after deferred has " +
         "been resolved") {
        val executeResultPromise: Promise[Int] = Promise()
        var counter = 0
        def processExecuteResult (executeResult: ExecuteResult) : Unit = {
          counter = counter + 1
          // Hack to allow callbacks to occur after meeting our assertion value
          if(counter == 2) {
            Thread.sleep(500)
            executeResultPromise.success(counter)
          }
        }

        val deferredExecution: DeferredExecution = DeferredExecution()
          .onResult(processExecuteResult)

        mockSendingKernelMessages(deferredExecution, executeReplyOk, executeReplyResult)
        //  register callback after code execution has completed
        deferredExecution.onResult(processExecuteResult)

        whenReady(executeResultPromise.future){ _ => counter should be(2) }

      }
      it("should not run onResult callbacks when ExecuteReply is a failure") {
        //  This promise should be resolved by the deferred
        implicit val executeReplyPromise: Promise[Try[ExecuteReplyPromise]] = Promise()
        //  This promise should not be resolved by the deferred
        implicit val executeResultPromise: Promise[Try[ExecuteResultPromise]] = Promise()

        val deferredExecution: DeferredExecution = DeferredExecution()
          .onError(processExecuteReply)
          .onResult(processExecuteResult)
        mockSendingKernelMessages(deferredExecution, executeReplyError, executeReplyResult)

        whenReady(executeReplyPromise.future) { _ =>
          executeResultPromise.isCompleted should be(false)
        }
      }
    }
    describe("onStream( callback )"){
      it("should execute all streaming callbacks") {
        var counter = 0
        val streamingResultPromise: Promise[Int] = Promise()
        def processStreamContent (streamContent: StreamContent) : Unit = {
          counter = counter + 1
          if (counter == 4)
            streamingResultPromise.success(counter)
        }

        val deferredExecution: DeferredExecution = DeferredExecution()
          .onStream(processStreamContent)
          .onStream(processStreamContent)

        deferredExecution.emitStreamContent(StreamContent("stdout","msg"))
        deferredExecution.emitStreamContent(StreamContent("stdout","msg2"))

        whenReady(streamingResultPromise.future){ counterValue =>
          counterValue should be(4)
        }
      }
    }
    
    describe("onError( callback )") {
      it("should run all onError callbacks") {
        implicit val executeReplyPromise: Promise[Try[ExecuteReplyPromise]] = Promise()
        val deferredExecution: DeferredExecution = DeferredExecution()
          .onError(processExecuteReply)

        mockSendingKernelMessages(deferredExecution, executeReplyError, executeReplyResult)

        whenReady(executeReplyPromise.future) {
          case Success(v) => assert(true)
          case Failure(exception: Throwable) =>
            fail("Promised resolved with failure while trying to " +
                 "process execute result.", exception)
          case unknownValue=>
            fail(s"Promised resolved with unknown value: ${unknownValue}")
        }
      }
      it("should not run onError callbacks when ExecuteReply is a success") {
        //  This promise and callback should not be executed by the deferred
        implicit val executeReplyPromise: Promise[Try[ExecuteReplyPromise]] = Promise()
        //  This promise and callback should be executed by the deferred
        implicit val executeResultPromise: Promise[Try[ExecuteResultPromise]] = Promise()

        val deferredExecution: DeferredExecution = DeferredExecution()
          .onError(processExecuteReply)
          .onResult(processExecuteResult)

        mockSendingKernelMessages(deferredExecution, executeReplyOk, executeReplyResult)

        whenReady(executeResultPromise.future) {
          case _ =>
            executeReplyPromise.isCompleted should be(false)
        }
      }
    }
  }
}
