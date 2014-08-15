package com.ibm.spark.magic.builtin

import java.io.File

import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.dependencies.{IncludeSparkContext, IncludeInterpreter}
import org.apache.spark.SparkContext

class AddJar extends MagicTemplate with IncludeInterpreter with IncludeSparkContext {
  /**
   * Downloads and adds the specified jars to the
   * interpreter/compiler/cluster classpaths.
   * @param code The lines containing the locations of the jars, separated by newline
   */
  override def executeCell(code: Seq[String]): String = {
    code.foreach(executeLine)

    "" // No output needed
  }

  /**
   * Downloads and adds the specified jar to the
   * interpreter/compiler/cluster classpaths.
   * @param code The line containing the location of the jar
   */
  override def executeLine(code: String): String = {
    val jarUrl = new File(code.trim()).toURI.toURL
    interpreter.addJars(jarUrl)
    sparkContext.addJar(jarUrl.getPath)

    "" // No output needed
  }
}
