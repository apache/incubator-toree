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

package integration

import java.io.OutputStream

import org.apache.toree.kernel.api.Kernel
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.apache.toree.kernel.protocol.v5.magic.PostProcessor
import org.apache.toree.utils.{MultiOutputStream}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class PostProcessorSpecForIntegration extends FunSpec with Matchers
  with BeforeAndAfter with MockitoSugar
{
  private var scalaInterpreter: ScalaInterpreter = _
  private var postProcessor: PostProcessor = _

  before {
    // TODO: Move instantiation and start of interpreter to a beforeAll
    //       for performance improvements
    scalaInterpreter = new ScalaInterpreter

    scalaInterpreter.init(mock[Kernel])

    postProcessor = new PostProcessor(scalaInterpreter)
  }

  describe("PostProcessor") {
    describe("#process") {
      describe("https://github.com/ibm-et/spark-kernel/issues/137") {
        it(Seq(
          "should not return a previous execution's result for a",
          "new execution with no result").mkString(" ")) {
          val result = scalaInterpreter.interpret("1+1")
          val postResult = postProcessor.process(result._2.left.get)

          // Imports in Scala do not create a new variable based on execution
          val result2 = scalaInterpreter.interpret("import java.lang._")
          val postResult2 = postProcessor.process(result2._2.left.get)

          postResult should not be (postResult2)
        }
      }
    }
  }
}
