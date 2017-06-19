/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */
package org.apache.toree.interpreter.broker

import org.apache.toree.interpreter.{ExecuteError, Results}
import org.scalatest.concurrent.Eventually
import scala.concurrent.Promise
import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

class BrokerTransformerSpec extends FunSpec with Matchers
  with OneInstancePerTest with Eventually
{
  private val brokerTransformer = new BrokerTransformer

  describe("BrokerTransformer") {
    describe("#transformToInterpreterResult") {
      it("should convert to success with result output if no failure") {
        val codeResultPromise = Promise[BrokerTypes.CodeResults]()

        val transformedFuture = brokerTransformer.transformToInterpreterResult(
          codeResultPromise.future
        )

        val successOutput = "some success"
        codeResultPromise.success(successOutput)

        eventually {
          val result = transformedFuture.value.get.get
          result should be((Results.Success, Left(Map("text/plain" -> successOutput))))
        }
      }

      it("should convert to error with broker exception if failure") {
        val codeResultPromise = Promise[BrokerTypes.CodeResults]()

        val transformedFuture = brokerTransformer.transformToInterpreterResult(
          codeResultPromise.future
        )

        val failureException = new BrokerException("some failure")
        codeResultPromise.failure(failureException)

        eventually {
          val result = transformedFuture.value.get.get
          result should be((Results.Error, Right(ExecuteError(
            name = failureException.getClass.getName,
            value = failureException.getLocalizedMessage,
            stackTrace = failureException.getStackTrace.map(_.toString).toList
          ))))
        }
      }
    }
  }
}
