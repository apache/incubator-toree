package com.ibm.spark.client

import java.io.File

import com.typesafe.config.{ConfigFactory, Config}
import joptsimple.{OptionParser, OptionSpec}

class SparkKernelClientOptions(args: Seq[String]) {
  def this(argsArray: Array[String]) = this(argsArray.toSeq)

  private val parser = new OptionParser()

  //  Options
  private val _profile =
    parser.accepts("profile", "path to IPython JSON connection file")
      .withOptionalArg().ofType(classOf[File])
  private val options = parser.parse(args: _*)

  private def has[T](spec: OptionSpec[T]): Boolean =
    options.has(spec)

  private def get[T](spec: OptionSpec[T]): Option[T] =
    Some(options.valueOf(spec)).filter(_ != null)

  val profile: Option[File] = get(_profile)

  def toConfig(): Config = {
    val profileConfig: Config = profile match {
      case Some(_) =>
        ConfigFactory.parseFile(profile.get)
      case None =>
        ConfigFactory.empty()
    }

    profileConfig.withFallback(ConfigFactory.load)
  }
}
