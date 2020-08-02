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

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.kernel.protocol.v5.client.boot.ClientBootstrap
import org.apache.toree.kernel.protocol.v5.client.boot.layers.{StandardHandlerInitialization, StandardSystemInitialization}
import org.apache.toree.kernel.protocol.v5.content._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

/**
 * This App demonstrates how to use the spark client in scala.
 * Use this class as a playground.
 */
object ScalaSparkClientUsage extends App {
  val config: Config = ConfigFactory.load(getClass.getClassLoader, "/kernel-profiles/IOPubIntegrationProfile.json")
  //  Setup
  val client = (new ClientBootstrap(config)
    with StandardSystemInitialization
    with StandardHandlerInitialization).createClient()

  def printStreamContent(content:StreamContent) = {
    println(s"Stream content on channel ${content.name} was: ${content.text}")
  }

  def printTextResult(result:ExecuteResult) = {
    println(s"ExecuteResult data was: ${result.data(MIMEType.PlainText)}")
  }

  def printError(reply:ExecuteReplyError) = {
    println(s"ExecuteReply error name was: ${reply.ename.get}")
  }

  //  A callback used to determine if the kernel is no longer responding
  client.heartbeat(() => {
      println("hb bad")
  })

  //  Assign a val
  client.execute("val z = 0")

  //  Print the val out to stdout
  client.execute("println(z)").onStream(printStreamContent)
  //  Stream content on channel stdout was: 0

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Non streaming message with result
  client.execute("1+1").onResult(printTextResult)
  //  ExecuteResult data was: res(\d)+: Int =  2

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Non streaming message with error
  client.execute("1/0")
    .onResult(printTextResult)
    .onError(printError)
  //  ExecuteReply error name was: java.lang.ArithmeticException

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Stream message and result
  client.execute("println(\"Hello World\"); 2 + 2")
    .onStream(printStreamContent)
    .onResult(printTextResult)
  //  Stream content on channel stdout was: "Hello World"
  //  ExecuteResult data was: res(\d)+: Int = 4

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Stream message with error
  client.execute("println(1/1); println(1/2); println(1/0);")
    .onStream(printStreamContent)
    .onError(printError)
  //  Stream content on channel stdout was: 1
  //  Stream content on channel stdout was: 0
  //  ExecuteReply error name was: java.lang.ArithmeticException

  //  Sleep so output does not overlap
  Thread.sleep(500)

  client.execute("val foo = 1+1; 2+2;")
    .onResult(printTextResult)
    .onStream(printStreamContent)
  //  ExecuteResult data was: foo: Int = 2
  //  res278: Int = 4

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Simulates calculating two numbers in the cluster and adding them client side
  def complexMath(x: Int, y: Int) = (x + 2) * y

  val xPromise: Promise[Int] = Promise()
  val yPromise: Promise[Int] = Promise()
  def parseResult(result: String): Int = {
    val intPattern = """res(\d)+: Int = (\d)+""".r
    intPattern findFirstIn result match {
      case Some(intPattern(resCount, res))  => Integer.parseInt(res)
      case None                             => 0
    }
  }

  //  Big data function 1
  client.execute("1+1").onResult((executeResult: ExecuteResult) => {
    val result: Int = parseResult(executeResult.data(MIMEType.PlainText))
    xPromise.success(result)
  })

  //  Big data function 2
  client.execute("3+3").onResult((executeResult: ExecuteResult) => {
    val result: Int = parseResult(executeResult.data(MIMEType.PlainText))
    yPromise.success(result)
  })

  val resultPromise = for {
    x <- xPromise.future
    y <- yPromise.future
  } yield complexMath(x, y)

  resultPromise.onComplete { x => println(s"Added result is $x") }
  //  Added result is 24

  //  Sleep so output does not overlap
  Thread.sleep(500)

  //  Running code on another thread
  val threadCode: String =
    """
      val s = new Thread(new Runnable {
        def run() {
        var x = 0
          while(x < 3) {
            Thread.sleep(1000)
            println("bean")
            x = x  + 1
          }
        }
      })
      s.start()
    """.stripMargin

  client.execute(threadCode).onStream(printStreamContent)
  //  Stream content on channel stdout was: bean
  //  Stream content on channel stdout was: bean
  //  Stream content on channel stdout was: bean

  //  Sleep so output does not overlap
  Thread.sleep(500)
}
