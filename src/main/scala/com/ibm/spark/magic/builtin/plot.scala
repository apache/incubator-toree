package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.magic.dependencies.IncludeInterpreter

class Plot extends MagicTemplate with IncludeInterpreter {

  // TODO don't pollute interpreter with png val
  private val png = "png123456789"

  /** Parses an argument value out of a string of format "(key=value )+" */
  private def argValue(input: String, name: String, default: String): String =
    s"$name=(\\S+)".r.findFirstMatchIn(input) match {
      case Some(value) => value.group(1)
      case None => default
    }

  /** DRY function to parse plot args and wrap code */
  private def wrapCode(rawArgs: String): String = {
    val argMatch = "^(.*?)chart=(.*)".r.findFirstMatchIn(rawArgs.trim)
    require(argMatch != None, "Must provide chart code to magic: \"%%plot [width=<int>] [height=<int>] chart=<code>\"")
    val (args, chart) = (argMatch.get.group(1), argMatch.get.group(2))

    val width =  argValue(args, "width", "400").toInt
    val height = argValue(args, "height", "300").toInt

    chartWrap(chart, width, height)
  }

  /** Wraps code around chart to convert to Base64 String */
  private def chartWrap(chart: String, width: Int, height: Int): String =
    s"""
        val baos = new java.io.ByteArrayOutputStream()
        org.jfree.chart.ChartUtilities.writeChartAsPNG(baos, $chart, $width, $height)
        val $png = javax.xml.bind.DatatypeConverter.printBase64Binary(baos.toByteArray)
    """.stripMargin

  /** Another DRY function to generate the plot */
  private def interpretWrappedCode(code: String): MagicOutput = {
    val (_, message) = interpreter.interpret(code)
    if (message.isLeft)
      interpreter.read(png) match {
        case Some(value) => MagicOutput(MIMEType.ImagePng -> value.asInstanceOf[String])
        case None => MagicOutput(MIMEType.PlainText -> "Error in plot magic!")
      }
    else MagicOutput(MIMEType.PlainText -> message.right.get.toString)
  }

  /**
   * Plots a chart. Plot magic declaration takes code to plot.
   *
   * Usage: %plot [width=<int>] [height=<int>] chart=<code>
   *
   * Example: %plot width=100 height=100 ChartFactory.createPieChart("name", data)
   *
   * Note: chart parameter must be final parameter
   * @param rawArgs Plot arguments
   * @return Base64 String representing the plot
   */
  override def executeLine(rawArgs: String): MagicOutput =
    interpretWrappedCode(wrapCode(rawArgs))

  /**
   * Plots a chart. Plot magic declaration takes code to plot.
   *
   * Usage:   %%plot [width=<int>] [height=<int>] chart=<code>
   *          ...
   *
   * Example: %%plot width=400 height=300 chart=createChart(args)
   *          ...
   *          val chart = ChartFactory.createPieChart("name", data)
   *
   * Note: chart parameter must be final parameter
   * @param code The list of code, separated by newlines. Magic line contains plot arguments.
   * @return Base64 String representing the plot
   */
  override def executeCell(code: Seq[String]): MagicOutput =
    interpretWrappedCode(code.tail.mkString("\n") + "\n" + wrapCode(code.head))
}