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

package system

import org.apache.toree.kernel.protocol.v5.client.SparkKernelClient
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Milliseconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import test.utils.root.{SparkKernelClientDeployer, SparkKernelDeployer}

/**
 * Tests all stdin-related functionality between the client and kernel.
 */
class StdinForSystemSpec extends FunSpec with Matchers with BeforeAndAfterAll
  with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(10, Seconds)),
    interval = scaled(Span(50, Milliseconds))
  )
  private val TestReplyString = "some value"

  private var client: SparkKernelClient = _

  // TODO: Enable once client and kernel have Akka versions that are matching
  // NOTE: This should be the case once we move to 1.2.1 as the Akka version
  //       was updated in Apache Spark to 2.3.4
  /*override protected def beforeAll(): Unit = {
    // Ensure that our one-time initialization happens
    SparkKernelDeployer.startInstance

    client = SparkKernelClientDeployer.startInstance
  }*/

  describe("Stdin for System") {
    describe("when the kernel requests input") {
      ignore("should receive input based on the client's response function") {
        var response: String = ""
        client.setResponseFunction((_, _) => TestReplyString)

        // Read in a chunk of data (our reply string) and return it as a string
        // to be verified by the test
        client.execute(
          """
            |var result: Array[Byte] = Array()
            |val in = kernel.in
            |do {
            |    result = result :+ in.read().toByte
            |} while(in.available() > 0)
            |new String(result)
          """.stripMargin
        ).onResult { result =>
          response = result.data("text/plain")
        }.onError { _ =>
          fail("Client execution to trigger kernel input request failed!")
        }

        eventually {
          response should contain (TestReplyString)
        }
      }
    }
  }

}
