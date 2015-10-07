/*
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package integration

import java.io.{ByteArrayOutputStream, OutputStream}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.interpreter.scala.{StandardTaskManagerProducer, StandardSparkIMainProducer, StandardSettingsProducer, ScalaInterpreter}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.interpreter.InterpreterActor
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.slf4j.Logger
import test.utils.UncaughtExceptionSuppression

import scala.concurrent.duration._
import test.utils.SparkContextProvider

object InterpreterActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class InterpreterActorSpecForIntegration extends TestKit(
  ActorSystem(
    "InterpreterActorSpec",
    ConfigFactory.parseString(InterpreterActorSpecForIntegration.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
  with MockitoSugar with UncaughtExceptionSuppression {

  private val output = new ByteArrayOutputStream()
  private val interpreter = new ScalaInterpreter(List(), output)
    with StandardSparkIMainProducer
    with StandardTaskManagerProducer
    with StandardSettingsProducer

  private val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("Test Kernel")

  private var context: SparkContext = _

  before {
    output.reset()
    interpreter.start()


    interpreter.doQuietly({
      conf.set("spark.repl.class.uri", interpreter.classServerURI)
      //context = new SparkContext(conf) with NoSparkLogging
      context = SparkContextProvider.sparkContext
      interpreter.bind(
        "sc", "org.apache.spark.SparkContext",
        context, List( """@transient"""))
    })
  }

  after {
    //  context is shared so dont stop it
    //    context.stop()
    interpreter.stop()
  }

  describe("Interpreter Actor with Scala Interpreter") {
    describe("#receive") {
      it("should return ok if the execute request is executed successfully") {
        val interpreterActor =
          system.actorOf(Props(
            classOf[InterpreterActor],
            new InterpreterTaskFactory(interpreter)
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        interpreterActor !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        val result =
          receiveOne(5.seconds)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isLeft should be (true)
        result.left.get shouldBe an [ExecuteOutput]
      }

      it("should return error if the execute request fails") {
        val interpreterActor =
          system.actorOf(Props(
            classOf[InterpreterActor],
            new InterpreterTaskFactory(interpreter)
          ))

        val executeRequest = ExecuteRequest(
          "...", false, false,
          UserExpressions(), false
        )

        interpreterActor !
          ((executeRequest, mock[KernelMessage], mock[OutputStream]))

        val result =
          receiveOne(5.seconds)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isRight should be (true)
        result.right.get shouldBe an [ExecuteError]
      }
    }
  }
}
