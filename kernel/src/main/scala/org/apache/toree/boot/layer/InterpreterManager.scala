package org.apache.toree.boot.layer

import org.apache.toree.kernel.api.KernelLike
import com.typesafe.config.Config
import org.apache.toree.interpreter._
import scala.collection.JavaConverters._

import org.slf4j.LoggerFactory

case class InterpreterManager(
  default: String = "Scala",
  interpreters: Map[String, Interpreter] = Map[String, Interpreter]()
) {


  def initializeInterpreters(kernel: KernelLike): Unit = {
    interpreters.values.foreach(interpreter =>
      interpreter.init(kernel)
    )
  }

  def addInterpreter(
    name:String,
    interpreter: Interpreter
  ): InterpreterManager = {
    copy(interpreters = interpreters + (name -> interpreter))
  }

  def defaultInterpreter: Option[Interpreter] = {
    interpreters.get(default)
  }
}

object InterpreterManager {

  protected val logger = LoggerFactory.getLogger(this.getClass.getName)

  def apply(config: Config): InterpreterManager = {
    val ip = config.getStringList("interpreter_plugins").asScala ++
      config.getStringList("default_interpreter_plugin").asScala

    val m = ip.foldLeft(Map[String, Interpreter]())( (acc, v) => {
      v.split(":") match {
        case Array(name, className) =>
          try {
            val i = Class
                .forName(className)
                .newInstance()
                .asInstanceOf[Interpreter]
            acc + (name -> i)
          }
          catch {
            case e:Throwable =>
              logger.error("Error loading interpreter class " + className)
              logger.error(e.getMessage())
              acc
          }
        case _ => acc
      }
    })

    val default = config.getString("default_interpreter")

    InterpreterManager(interpreters = m, default = default)
  }
}
