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
            val i = instantiate(className, config)
            acc + (name -> i)
          }
          catch {
            case e:Throwable =>
              logger.error("Error loading interpreter class " + className)
              logger.error(e.getMessage())
              //acc
              throw e
          }
        case _ => acc
      }
    })

    val default = config.getString("default_interpreter")

    InterpreterManager(interpreters = m, default = default)
  }

  /**
   * instantiate will look for a constructor that take a Config. If available, will
   * call that, else it will assume that there is a default empty constructor.
   * @param className
   * @param config
   * @return
   */
  private def instantiate(className:String, config:Config):Interpreter = {
    try {
      Class
        .forName(className)
        .getDeclaredConstructor(Class.forName("com.typesafe.config.Config"))
        .newInstance(config).asInstanceOf[Interpreter]
    }
    catch {
      case e: NoSuchMethodException =>
        logger.debug("Using default constructor for class " + className)
        Class
          .forName(className)
          // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
          .getDeclaredConstructor().newInstance().asInstanceOf[Interpreter]
    }

  }

}
