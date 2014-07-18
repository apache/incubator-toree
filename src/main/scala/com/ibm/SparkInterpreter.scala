package com.ibm

import org.apache.spark.repl.SparkIMain
import org.apache.spark.{SparkConf, SparkContext}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{JPrintWriter, LoopCommands}

class SparkInterpreter(
    protected val master: Option[String],
    settings: Settings,
    protected val output: JPrintWriter
) extends SparkIMain(settings, output)
  with LoopCommands // For Result object
{
  /*
  private def getMaster(): String = {
    val master = this.master match {
      case Some(m) => m
      case None =>
        val envMaster = sys.env.get("MASTER")
        val propMaster = sys.props.get("spark.master")
        propMaster.orElse(envMaster).getOrElse("local[*]")
    }
    master
  }

  def createSparkContext(): SparkContext = {
    val execUri = System.getenv("SPARK_EXECUTOR_URI")
    val jars = SparkILoop.getAddedJars
    val conf = new SparkConf()
      .setMaster(getMaster())
      .setAppName("Spark shell")
      .setJars(jars)
      .set("spark.repl.class.uri", intp.classServer.uri)
    if (execUri != null) {
      conf.set("spark.executor.uri", execUri)
    }
    if (System.getenv("SPARK_HOME") != null) {
      conf.setSparkHome(System.getenv("SPARK_HOME"))
    }
    sparkContext = new SparkContext(conf)
    logInfo("Created spark context..")
    sparkContext
  }
  */
}
