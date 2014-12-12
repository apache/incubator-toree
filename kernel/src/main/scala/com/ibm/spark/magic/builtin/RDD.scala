/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.magic.builtin

import com.ibm.spark.interpreter.{ExecuteAborted, ExecuteError}
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic._
import com.ibm.spark.magic.dependencies.IncludeInterpreter
import com.ibm.spark.utils.LogLike
import com.ibm.spark.utils.json.RddToJson
import org.apache.spark.sql.SchemaRDD

/**
 * Temporary magic to show an RDD as JSON
 */
class RDD extends MagicTemplate with IncludeInterpreter with LogLike {

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
              logger.error(e.getMessage, e)
              MagicOutput(MIMEType.PlainText ->
                ("An error occurred converting RDD to JSON.\n"+e.getMessage))
        }
      } else MagicOutput(MIMEType.PlainText -> result)

    // NOTE: Forced to construct and throw exception to trigger proper reporting
    // TODO: Refactor to potentially have interpreter throw exception
    } else {
      message.right.get match {
        case ex: ExecuteError => throw new Throwable(ex.value)
        case ex: ExecuteAborted => throw new Throwable("RDD magic aborted!")
      }
    }

    //else MagicOutput(MIMEType.PlainText -> message.right.get.toString)
  }

  override def executeLine(code: String): MagicOutput = convertToJson(code)

  override def executeCell(code: Seq[String]): MagicOutput =
    convertToJson(code.mkString("\n"))
}