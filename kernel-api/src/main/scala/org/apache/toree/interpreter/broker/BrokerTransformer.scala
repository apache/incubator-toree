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

import org.apache.toree.interpreter.InterpreterTypes.ExecuteOutput
import org.apache.toree.interpreter.Results.Result
import org.apache.toree.interpreter.broker.BrokerTypes.CodeResults
import org.apache.toree.interpreter.{ExecuteError, ExecuteFailure, Results}

import scala.concurrent.Future

/**
 * Represents a utility that can transform raw broker information to
 * kernel information.
 */
class BrokerTransformer {
  /**
   * Transforms a pure result containing output information into a form that
   * the interpreter interface expects.
   *
   * @param futureResult The raw result as a future
   *
   * @return The transformed result as a future
   */
  def transformToInterpreterResult(futureResult: Future[CodeResults]):
    Future[(Result, Either[ExecuteOutput, ExecuteFailure])] =
  {
    import scala.concurrent.ExecutionContext.Implicits.global

    futureResult
      .map(results => (Results.Success, Left(Map("text/plain" -> results))))
      .recover({ case ex: BrokerException =>
        (Results.Error, Right(ExecuteError(
          name = ex.getClass.getName,
          value = ex.getLocalizedMessage,
          stackTrace = ex.getStackTrace.map(_.toString).toList
        )))
      })
  }
}
