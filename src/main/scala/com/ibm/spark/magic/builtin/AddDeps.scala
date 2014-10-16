package com.ibm.spark.magic.builtin

import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.magic.dependencies.{IncludeSparkContext, IncludeOutputStream, IncludeInterpreter}
import com.ibm.spark.dependencies.IvyDependencyDownloader
import com.ibm.spark.kernel.protocol.v5.MIMEType
import java.io.PrintStream
import com.ibm.spark.SparkKernelOptions
import com.typesafe.config.Config

class AddDeps extends MagicTemplate with IncludeInterpreter
  with IncludeOutputStream with IncludeSparkContext {
  /**
   * Execute a magic representing a cell magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    code.foreach(executeLine)
    MagicOutput()
  }

  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  override def executeLine(code: String): MagicOutput = {
    // where do we want to put this stuff? should we isolate dependencies
    // per user session?
    val ivyDir = new SparkKernelOptions(Seq()).toConfig.getString("ivy_local")

    val downloader = new IvyDependencyDownloader(
      "http://repo1.maven.org/maven2/", "/tmp/.ivy2"
    )
    val stream = new PrintStream(outputStream)
    downloader.setPrintStream(stream)

    // [0] = organization, [1] = artifact id, [2] = version
    val parts = code.split(" ")

    // require a version or use the most recent if omitted?
    if (parts.size != 3) {
      val err =
        """Invalid syntax.
          |Specify dependencies as my.company artifact-id version""".stripMargin
      return MagicOutput(MIMEType.PlainText -> err)
    }

    // get the jars and hold onto the paths at which they reside
    val urls = downloader.retrieve(parts(0), parts(1), parts(2))

    // add the jars to the interpreter and spark context
    interpreter.addJars(urls:_*)
    urls.foreach(url => sparkContext.addJar(url.getPath))

    // TODO: report issues, etc, to the user or is the ivy output enough?
    MagicOutput()
  }
}
