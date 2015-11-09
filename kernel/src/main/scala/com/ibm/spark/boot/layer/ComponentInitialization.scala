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

package com.ibm.spark.boot.layer

import java.util
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorRef
import com.ibm.spark.comm.{CommManager, KernelCommManager, CommRegistrar, CommStorage}
import com.ibm.spark.dependencies.{DependencyDownloader, IvyDependencyDownloader}
import com.ibm.spark.global
import com.ibm.spark.interpreter._
import com.ibm.spark.kernel.api.{KernelLike, Kernel}
import com.ibm.spark.kernel.interpreter.pyspark.PySparkInterpreter
import com.ibm.spark.kernel.interpreter.sparkr.SparkRInterpreter
import com.ibm.spark.kernel.interpreter.scala.{TaskManagerProducerLike, StandardSparkIMainProducer, StandardSettingsProducer, ScalaInterpreter}
import com.ibm.spark.kernel.interpreter.sql.SqlInterpreter
import com.ibm.spark.kernel.protocol.v5.KMBuilder
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.stream.KernelOutputStream
import com.ibm.spark.magic.MagicLoader
import com.ibm.spark.magic.builtin.BuiltinLoader
import com.ibm.spark.magic.dependencies.DependencyMap
import com.ibm.spark.utils.{MultiClassLoader, TaskManager, KeyValuePairUtils, LogLike}
import com.typesafe.config.Config
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.JavaConverters._

import scala.util.Try

/**
 * Represents the component initialization. All component-related pieces of the
 * kernel (non-actors) should be created here. Limited items should be exposed.
 */
trait ComponentInitialization {
  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param appName The name of the "application" for Spark
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, appName: String, actorLoader: ActorLoader
  ): (CommStorage, CommRegistrar, CommManager, Interpreter,
    Kernel, DependencyDownloader, MagicLoader,
    collection.mutable.Map[String, ActorRef])
}

/**
 * Represents the standard implementation of ComponentInitialization.
 */
trait StandardComponentInitialization extends ComponentInitialization {
  this: LogLike =>

  /**
   * Initializes and registers all components (not needed by bare init).
   *
   * @param config The config used for initialization
   * @param appName The name of the "application" for Spark
   * @param actorLoader The actor loader to use for some initialization
   */
  def initializeComponents(
    config: Config, appName: String, actorLoader: ActorLoader
  ) = {
    val (commStorage, commRegistrar, commManager) =
      initializeCommObjects(actorLoader)
    val interpreter = initializeInterpreter(config)

    val dependencyDownloader = initializeDependencyDownloader(config)
    val magicLoader = initializeMagicLoader(
      config, interpreter, dependencyDownloader)
    val manager =  InterpreterManager(config)
      .addInterpreter("Scala",interpreter)
    val kernel = initializeKernel(
      config, actorLoader, manager, commManager, magicLoader
    )
    val responseMap = initializeResponseMap()


    /*
    // NOTE: Tested via initializing the following and returning this
    //       interpreter instead of the Scala one
    val pySparkInterpreter = new PySparkInterpreter(kernel)
    //pySparkInterpreter.start()
    kernel.data.put("PySpark", pySparkInterpreter)

    // NOTE: Tested via initializing the following and returning this
    //       interpreter instead of the Scala one
    val sparkRInterpreter = new SparkRInterpreter(kernel)
    //sparkRInterpreter.start()
    kernel.data.put("SparkR", sparkRInterpreter)

    val sqlInterpreter = new SqlInterpreter(kernel)
    //sqlInterpreter.start()
    kernel.data.put("SQL", sqlInterpreter)


    val plugins = initializeInterpreterPlugins(kernel, config)

    kernel.data.putAll(plugins.asJava)

    // Add Scala to available data map
    kernel.data.put("Scala", interpreter)
    val defaultInterpreter: Interpreter =
      config.getString("default_interpreter").toLowerCase match {
        case "scala" =>
          logger.info("Using Scala interpreter as default!")
          interpreter.doQuietly {
            interpreter.bind(
              "kernel", "com.ibm.spark.kernel.api.Kernel",
              kernel, List( """@transient implicit""")
            )
          }
          interpreter
        case "pyspark" =>
          logger.info("Using PySpark interpreter as default!")
          pySparkInterpreter
        case "sparkr" =>
          logger.info("Using SparkR interpreter as default!")
          sparkRInterpreter
        case "sql" =>
          logger.info("Using SQL interpreter as default!")
          sqlInterpreter
        case p if(kernel.data.containsKey(p)) =>
          kernel.data.get(p).asInstanceOf[Interpreter]
        case unknown =>
          logger.warn(s"Unknown interpreter '$unknown'! Defaulting to Scala!")
          interpreter
      }

    */
    //kernel.interpreter = defaultInterpreter
    initializeSparkContext(config, kernel, appName)

    (commStorage, commRegistrar, commManager,
      manager.defaultInterpreter.getOrElse(null), kernel,
      dependencyDownloader, magicLoader, responseMap)

  }

  def initializeInterpreterPlugins(
    kernel: KernelLike,
    config: Config
  ): Map[String, Interpreter] = {
    val p = config
      .getStringList("interpreter_plugins")
      .listIterator().asScala

    p.foldLeft(Map[String, Interpreter]())( (acc, v) => {
      v.split(":") match {
        case Array(name, className) =>
          try {
            acc + (name -> Class
              .forName(className)
              .getConstructor(classOf[KernelLike])
              .newInstance(kernel)
              .asInstanceOf[Interpreter])
          }
          catch {
            case _:Throwable => acc
          }
        case _ => acc
      }
    })
  }

  def initializeSparkContext(config:Config, kernel:Kernel, appName:String) = {
    if(!config.getBoolean("nosparkcontext")) {
      kernel.createSparkContext(config.getString("spark.master"), appName)
    }
  }

  private def initializeCommObjects(actorLoader: ActorLoader) = {
    logger.debug("Constructing Comm storage")
    val commStorage = new CommStorage()

    logger.debug("Constructing Comm registrar")
    val commRegistrar = new CommRegistrar(commStorage)

    logger.debug("Constructing Comm manager")
    val commManager = new KernelCommManager(
      actorLoader, KMBuilder(), commRegistrar)

    (commStorage, commRegistrar, commManager)
  }

  private def initializeDependencyDownloader(config: Config) = {
    val dependencyDownloader = new IvyDependencyDownloader(
      "http://repo1.maven.org/maven2/", config.getString("ivy_local")
    )

    dependencyDownloader
  }

  protected def initializeInterpreter(config: Config) = {
    val interpreterArgs = config.getStringList("interpreter_args").asScala.toList
    val maxInterpreterThreads = config.getInt("max_interpreter_threads")

    logger.info(
      s"Constructing interpreter with $maxInterpreterThreads threads and " +
      "with arguments: " + interpreterArgs.mkString(" "))
    val interpreter = new ScalaInterpreter(interpreterArgs, Console.out)
      with StandardSparkIMainProducer
      with TaskManagerProducerLike
      with StandardSettingsProducer {
      override def newTaskManager(): TaskManager =
        new TaskManager(maximumWorkers = maxInterpreterThreads)
    }

    logger.debug("Starting interpreter")
    interpreter.start()

    interpreter
  }

  protected[layer] def initializeSqlContext(sparkContext: SparkContext) = {
    val sqlContext: SQLContext = try {
      logger.info("Attempting to create Hive Context")
      val hiveContextClassString =
        "org.apache.spark.sql.hive.HiveContext"

      logger.debug(s"Looking up $hiveContextClassString")
      val hiveContextClass = Class.forName(hiveContextClassString)

      val sparkContextClass = classOf[SparkContext]
      val sparkContextClassName = sparkContextClass.getName

      logger.debug(s"Searching for constructor taking $sparkContextClassName")
      val hiveContextContructor =
        hiveContextClass.getConstructor(sparkContextClass)

      logger.debug("Invoking Hive Context constructor")
      hiveContextContructor.newInstance(sparkContext).asInstanceOf[SQLContext]
    } catch {
      case _: Throwable =>
        logger.warn("Unable to create Hive Context! Defaulting to SQL Context!")
        new SQLContext(sparkContext)
    }

    sqlContext
  }

  protected[layer] def updateInterpreterWithSqlContext(
    sqlContext: SQLContext, interpreter: Interpreter
  ): Unit = {
    interpreter.doQuietly {
      // TODO: This only adds the context to the main interpreter AND
      //       is limited to the Scala interpreter interface
      logger.debug("Adding SQL Context to main interpreter")
      interpreter.bind(
        "sqlContext",
        classOf[SQLContext].getName,
        sqlContext,
        List( """@transient""")
      )

      sqlContext
    }
  }

  protected def initializeResponseMap(): collection.mutable.Map[String, ActorRef] =
    new ConcurrentHashMap[String, ActorRef]().asScala

  private def initializeKernel(
    config: Config,
    actorLoader: ActorLoader,
    interpreterManager: InterpreterManager,
    commManager: CommManager,
    magicLoader: MagicLoader
  ) = {
    val kernel = new Kernel(
      config,
      actorLoader,
      interpreterManager,
      commManager,
      magicLoader
    )
    /*
    interpreter.doQuietly {
      interpreter.bind(
        "kernel", "com.ibm.spark.kernel.api.Kernel",
        kernel, List( """@transient implicit""")
      )
    }
    */
    magicLoader.dependencyMap.setKernel(kernel)

    kernel
  }

  private def initializeMagicLoader(
    config: Config, interpreter: Interpreter,
    dependencyDownloader: DependencyDownloader
  ) = {
    logger.debug("Constructing magic loader")

    logger.debug("Building dependency map")
    val dependencyMap = new DependencyMap()
      .setInterpreter(interpreter)
      .setKernelInterpreter(interpreter) // This is deprecated
      .setDependencyDownloader(dependencyDownloader)
      .setConfig(config)

    logger.debug("Creating BuiltinLoader")
    val builtinLoader = new BuiltinLoader()

    val magicUrlArray = config.getStringList("magic_urls").asScala
      .map(s => new java.net.URL(s)).toArray

    if (magicUrlArray.isEmpty)
      logger.warn("No external magics provided to MagicLoader!")
    else
      logger.info("Using magics from the following locations: " +
        magicUrlArray.map(_.getPath).mkString(","))

    val multiClassLoader = new MultiClassLoader(
      builtinLoader,
      interpreter.classLoader
    )

    logger.debug("Creating MagicLoader")
    val magicLoader = new MagicLoader(
      dependencyMap = dependencyMap,
      urls = magicUrlArray,
      parentLoader = multiClassLoader
    )
    magicLoader.dependencyMap.setMagicLoader(magicLoader)
    magicLoader
  }
}
