package com.ibm.spark.boot.layer

import com.ibm.spark.kernel.api.KernelLike
import com.typesafe.config.Config
import com.ibm.spark.interpreter._
import scala.collection.JavaConverters._

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

  def defaultInterpreter(): Option[Interpreter] = {
    interpreters.get(default)
  }
}

object InterpreterManager {

  def apply(config: Config): InterpreterManager = {
    val p = config
      .getStringList("interpreter_plugins")
      .listIterator().asScala

    val m = p.foldLeft(Map[String, Interpreter]())( (acc, v) => {
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
            case _:Throwable => acc
          }
        case _ => acc
      }
    })

    val default = config.getString("default_interpreter")

    InterpreterManager(interpreters = m, default = default)
  }
}
