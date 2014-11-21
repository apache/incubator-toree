package com.ibm.spark.magic.builtin

import java.io.PrintStream

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.{IncludeInterpreter, IncludeOutputStream}
import com.ibm.spark.utils.ArgumentParsingSupport
import org.slf4j.LoggerFactory

/**
 * Temporary magic to show chart as image
 */
class Plot extends MagicTemplate with IncludeInterpreter
  with IncludeOutputStream with ArgumentParsingSupport {

  private lazy val printStream = new PrintStream(outputStream)

  // TODO don't pollute interpreter with png val
  private val png = "png123456789"

  // Option to specify height of chart
  private val height =
    parser.accepts("height", "height of the generated chart")
      .withOptionalArg().ofType(classOf[Int]).defaultsTo(300)

  // Option to specify width of chart
  private val width =
    parser.accepts("width", "width of the generated chart")
      .withOptionalArg().ofType(classOf[Int]).defaultsTo(400)

  // Option for chart code
  private val chart =
    parser.accepts("chart", "code to generate the chart (no spaces!)")
      .withRequiredArg().ofType(classOf[String])

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
        case Some(value) =>
          MagicOutput(MIMEType.ImagePng -> value.asInstanceOf[String])
        case None =>
          MagicOutput(MIMEType.PlainText -> "Error in plot magic!")
      }
    else
      MagicOutput(MIMEType.PlainText -> message.right.get.toString)
  }

  /**
   * Unsupported for this magic.
   * @param code The list of code, separated by newlines.
   * @return Base64 String representing the plot
   */
  override def executeCell(code: Seq[String]): MagicOutput = {
    MagicOutput()
  }

  /**
   * Plots a chart. Plot magic declaration takes code to plot.
   *
   * Usage: %plot [width=<int>] [height=<int>] chart=<code>
   *
   * Example:
   *   %plot --width=100 --height=100 --chart=ChartFactory.createPieChart("name",data)
   *
   * Note: chart parameter must be final parameter
   * @param code Plot arguments
   * @return Base64 String representing the plot
   */
  override def executeLine(code: String): MagicOutput = {
    val logger = LoggerFactory.getLogger(this.getClass)
    logger.info(code)
    val nonOptionArgs = parseArgs(code)
    has(chart) match {
      case false =>
        printHelp(
          printStream, """%Plot --chart=<string> --height=<int> --width=<int>"""
        )
        MagicOutput()
      case true =>
        interpretWrappedCode(chartWrap(chart.get, width.get, height.get))
    }
  }
}