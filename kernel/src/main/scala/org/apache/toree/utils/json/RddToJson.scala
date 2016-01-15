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

package org.apache.toree.utils.json

import org.apache.spark.sql.{DataFrame, SchemaRDD}
import play.api.libs.json.{JsObject, JsString, Json}

/**
 * Utility to convert RDD to JSON.
 */
object RddToJson {

  /**
   * Converts a SchemaRDD to a JSON table format.
   *
   * @param rdd The schema rdd (now a dataframe) to convert
   *
   * @return The resulting string representing the JSON
   */
  def convert(rdd: DataFrame, limit: Int = 10): String =
    JsObject(Seq(
      "type" -> JsString("rdd/schema"),
      "columns" -> Json.toJson(rdd.schema.fieldNames),
      "rows" -> Json.toJson(rdd.map(row =>
        row.toSeq.map(_.toString).toArray).take(limit))
    )).toString()
}