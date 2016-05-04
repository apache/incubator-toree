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

package org.apache.toree.utils

import org.apache.spark.sql.DataFrame
import org.apache.toree.plugins.Plugin
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Try}
import org.apache.toree.plugins.annotations.Init

class DataFrameConverter extends Plugin with LogLike {
  @Init def init() = {
    register(this)
  }

  def convert(
     df: DataFrame, outputType: String, limit: Int = 10
  ): Try[String] = {
    Try(
      outputType.toLowerCase() match {
        case "html" =>
          convertToHtml(df = df, limit = limit)
        case "json" =>
          convertToJson(df = df, limit = limit)
        case "csv" =>
          convertToCsv(df = df, limit = limit)
      }
    )
  }

  private def convertToHtml(df: DataFrame, limit: Int = 10): String = {
      val columnFields = df.schema.fieldNames.map(columnName => {
        s"<th>${columnName}</th>"
      }).reduce(_ + _)
      val columns = s"<tr>${columnFields}</tr>"
      val rows = df.map(row => {
        val fieldValues = row.toSeq.map(field => {
         s"<td>${field.toString}</td>"
        }).reduce(_ + _)
        s"<tr>${fieldValues}</tr>"
      }).take(limit).reduce(_ + _)
      s"<table>${columns}${rows}</table>"
  }

  private def convertToJson(df: DataFrame, limit: Int = 10): String = {
    JsObject(Seq(
      "columns" -> Json.toJson(df.schema.fieldNames),
      "rows" -> Json.toJson(df.map(row =>
        row.toSeq.map(_.toString).toArray).take(limit))
    )).toString()
  }

  private def convertToCsv(df: DataFrame, limit: Int = 10): String = {
      val headers = df.schema.fieldNames.reduce(_ + "," + _)
      val rows = df.map(row => {
        row.toSeq.map(field => field.toString).reduce(_ + "," + _)
      }).take(limit).reduce(_ + "\n" + _)
      s"${headers}\n${rows}"
  }
}