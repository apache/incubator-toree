package com.ibm.spark.utils.json

import org.apache.spark.sql.SchemaRDD
import play.api.libs.json.{JsObject, JsString, Json}

/**
 * Utility to convert RDD to JSON.
 */
object RddToJson {

  /**
   * Converts a SchemaRDD to a JSON table format.
   * @param rdd
   * @return
   */
  def convert(rdd: SchemaRDD, limit: Int = 10): String =
    JsObject(Seq(
      "type" -> JsString("rdd/schema"),
      "columns" -> Json.toJson(rdd.schema.fieldNames.toArray),
      "rows" -> Json.toJson(rdd.map(row => row.foldLeft(Array[String]()) {
        (acc, i) => acc :+ i.toString
      }).take(limit))
    )).toString
}