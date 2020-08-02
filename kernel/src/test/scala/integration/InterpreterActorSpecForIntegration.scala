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

import java.io.{ByteArrayOutputStream, OutputStream}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.apache.toree.Main
import org.apache.toree.interpreter._
import org.apache.toree.kernel.api.{DisplayMethodsLike, KernelLike}
import org.apache.toree.kernel.interpreter.scala.ScalaInterpreter
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content._
import org.apache.toree.kernel.protocol.v5.interpreter.InterpreterActor
import org.apache.toree.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import test.utils.UncaughtExceptionSuppression
import test.utils.MaxAkkaTestTimeout

object InterpreterActorSpecForIntegration {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class InterpreterActorSpecForIntegration extends TestKit(
  ActorSystem(
    "InterpreterActorSpec",
    ConfigFactory.parseString(InterpreterActorSpecForIntegration.config),
    Main.getClass.getClassLoader
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
  with MockitoSugar with UncaughtExceptionSuppression {

  private val output = new ByteArrayOutputStream()
  private val interpreter = new ScalaInterpreter {
    override protected def bindKernelVariable(kernel: KernelLike): Unit = { }
  }


  before {
    output.reset()
    // interpreter.start()
    val mockDisplayMethods = mock[DisplayMethodsLike]
    val mockKernel = mock[KernelLike]
    doReturn(mockDisplayMethods).when(mockKernel).display
    interpreter.init(mockKernel)

    interpreter.doQuietly({
      //context = new SparkContext(conf) with NoSparkLogging
      //context = SparkContextProvider.sparkContext
      //interpreter.bind(
      //  "sc", "org.apache.spark.SparkContext",
      //  context, List( """@transient"""))
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
          receiveOne(MaxAkkaTestTimeout)
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
          receiveOne(MaxAkkaTestTimeout)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isRight should be (true)
        result.right.get shouldBe an [ExecuteError]
      }
    }
  }
}
