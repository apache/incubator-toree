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

package examples

import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.kernel.protocol.v5.client.boot.ClientBootstrap
import org.apache.toree.kernel.protocol.v5.client.boot.layers.{StandardHandlerInitialization, StandardSystemInitialization}
import org.apache.toree.kernel.protocol.v5.content._
import com.typesafe.config.{Config, ConfigFactory}

object DocumentationExamples extends App {
  /************
  *   Setup   *
  ************/
  //  Create the connection information
  val profileJSON: String = """
  {
      "stdin_port" : 48691,
      "control_port" : 44808,
      "hb_port" : 49691,
      "shell_port" : 40544,
      "iopub_port" : 43462,
      "ip" : "192.168.44.44",
      "transport" : "tcp",
      "signature_scheme" : "hmac-sha256",
      "key" : ""
  }
  """.stripMargin
  val config: Config = ConfigFactory.parseString(profileJSON)
  val client = (new ClientBootstrap(config)
      with StandardSystemInitialization
      with StandardHandlerInitialization).createClient()

  /********************
  *   Executing Code  *
  ********************/
  //  Create a variable, z, and assign a value to it
  client.execute("val z = 0")
  Thread.sleep(500)
  //  Perform some computation
  client.execute("1 + 1")
  Thread.sleep(500)
  //  Print some message
  client.execute("println(\"Hello, World\")")
  Thread.sleep(500)

  /**********************************
  *   Receiving Results onResult    *
  **********************************/
  def printResult(result: ExecuteResult) = {
    println(s"Result was: ${result.data.get(MIMEType.PlainText).get}")
  }
  //  Create a variable, z, and assign a value to it
  client.execute("val z = 0").onResult(printResult)
  Thread.sleep(500)
  //  Perform some computation, and print it twice
  client.execute("1 + 1").onResult(printResult).onResult(printResult)
  Thread.sleep(500)
  //  The callback will never be invoked
  client.execute("someUndefinedVariable").onResult(printResult)
  Thread.sleep(500)

  /****************************************
  *   Receiving Print Streams onStream    *
  ****************************************/
  def printStreamContent(content:StreamContent) = {
    println(s"Stream content was: ${content.text}")
  }
  client.execute("println(1/1)").onStream(printStreamContent)
  Thread.sleep(500)
  client.execute("println(\"Hello, World\")").onStream(printStreamContent)
  Thread.sleep(500)

  /********************************
  *   Handling Errors: onError    *
  ********************************/
  def printError(reply:ExecuteReplyError) = {
    println(s"Error was: ${reply.ename.get}")
  }
  //  Error from executing a statement
  client.execute("1/0").onError(printError)
  Thread.sleep(500)
  //  Error from invoking a println
  client.execute("println(someUndefinedVar").onError(printError)
  Thread.sleep(500)
  /*
    Output should be:
      Result was: z: Int = 0
      Result was: res*: Int = 2
      Result was: res*: Int = 2
      Stream content was: 1

      Stream content was: Hello, World

      Error was: java.lang.ArithmeticException
      Error was: Syntax Error.
   */


  /***************************************************************************
  *   Notification of Successful Code Completion : onSuccessfulCompletion    *
  ***************************************************************************/
  def printSuccess(executeReplyOk: ExecuteReplyOk) = {
    println(s"Successful code completion")
  }
  //  onSuccess will be called
  client.execute("33*33")
    .onSuccess(printSuccess)
    .onError(printError)
  Thread.sleep(500)
  //  onSuccess will not be called
  client.execute("1/0").onSuccess {
   executeReplyOk =>
      println("This is never called.")
  }.onError(printError)
  Thread.sleep(500)
  /*
    Output should be:
      Result was: res*: Int = 1089

      Error was: java.lang.ArithmeticException
  */

  //  Wait for all the executions to complete
  Thread.sleep(30000)
  //  Properly shutdown the client and exit the application
  client.shutdown()
}
