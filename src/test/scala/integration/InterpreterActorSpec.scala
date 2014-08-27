package integration

import java.io.{ByteArrayOutputStream, OutputStream}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.interpreter.{ExecuteError, ExecuteOutput, ScalaInterpreter}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.ibm.spark.kernel.protocol.v5.interpreter.InterpreterActor
import com.ibm.spark.kernel.protocol.v5.interpreter.tasks.InterpreterTaskFactory
import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import org.slf4j.Logger

import scala.concurrent.duration._

object InterpreterActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class InterpreterActorSpec extends TestKit(
  ActorSystem(
    "InterpreterActorSpec",
    ConfigFactory.parseString(InterpreterActorSpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
  with MockitoSugar
{

  private val output = new ByteArrayOutputStream()
  private val interpreter = ScalaInterpreter(List(), output)

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
      context = new SparkContext(conf) {
        override protected def log: Logger =
          org.slf4j.helpers.NOPLogger.NOP_LOGGER
      }
      interpreter.bind(
        "sc", "org.apache.spark.SparkContext",
        context, List( """@transient"""))
    })
  }

  after {
    context.stop()
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

        interpreterActor ! ((executeRequest, mock[OutputStream]))

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
          "val x =", false, false,
          UserExpressions(), false
        )

        interpreterActor ! ((executeRequest, mock[OutputStream]))

        val result =
          receiveOne(5.seconds)
            .asInstanceOf[Either[ExecuteOutput, ExecuteError]]

        result.isRight should be (true)
        result.right.get shouldBe an [ExecuteError]
      }
    }
  }
}
