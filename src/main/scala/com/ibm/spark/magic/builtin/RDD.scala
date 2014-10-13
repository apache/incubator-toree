package com.ibm.spark.magic.builtin

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.MagicOutput
import com.ibm.spark.magic.dependencies.IncludeInterpreter
import com.ibm.spark.utils.json.RddToJson
import org.apache.spark.sql.SchemaRDD

/**
 * Temporary magic to show an RDD as JSON
 */
class RDD extends MagicTemplate with IncludeInterpreter {

  private def convertToJson(code: String) = {
    val (_, message) = interpreter.interpret(code)
    val rddRegex = "(^|\\n)(res\\d+): org.apache.spark.sql.SchemaRDD =".r

    if (message.isLeft) {
      val result = message.left.get.toString
      if (rddRegex.findFirstIn(result).nonEmpty) {
        val matchData = rddRegex.findFirstMatchIn(result).get
        try {
          val rdd = interpreter.read(matchData.group(2)).get.asInstanceOf[SchemaRDD]
          MagicOutput(MIMEType.ApplicationJson -> RddToJson.convert(rdd))
        } catch {
          case e: Exception =>
            e.printStackTrace
            MagicOutput(MIMEType.PlainText -> ("An error occurred converting RDD to JSON.\n"+e.getMessage))
        }
      } else MagicOutput(MIMEType.PlainText -> result)
    } else MagicOutput(MIMEType.PlainText -> message.right.get.toString)
  }

  override def executeLine(code: String): MagicOutput = convertToJson(code)

  override def executeCell(code: Seq[String]): MagicOutput =
    convertToJson(code.tail.mkString("\n"))
}