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

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsArray, JsString, Json}

class DataFrameConverterSpec extends FunSpec with MockitoSugar with Matchers {
  val dataFrameConverter: DataFrameConverter = new DataFrameConverter
  val mockDataFrame = mock[DataFrame]
  val mockRdd = mock[RDD[Any]]
  val mockStruct = mock[StructType]
  val columns = Seq("foo", "bar").toArray
  val rowsOfArrays = Array( Array("a", "b"), Array("c", "d") )
  val rowsOfStrings = Array("test1","test2")
  val rowsOfString = Array("test1")

  doReturn(mockStruct).when(mockDataFrame).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockDataFrame).rdd
  doReturn(mockRdd).when(mockRdd).map(any())(any())
  doReturn(rowsOfArrays).when(mockRdd).take(anyInt())

  describe("DataFrameConverter") {
    describe("#convert") {
      it("should convert to a valid JSON object") {
        val someJson = dataFrameConverter.convert(mockDataFrame, "json")
        val jsValue = Json.parse(someJson.get)
        jsValue \ "columns" should be (JsArray(Seq(JsString("foo"), JsString("bar"))))
        jsValue \ "rows" should be (JsArray(Seq(
          JsArray(Seq(JsString("a"), JsString("b"))),
          JsArray(Seq(JsString("c"), JsString("d")))))
        )
      }
      it("should convert to csv") {
        doReturn(rowsOfStrings).when(mockRdd).take(anyInt())
        val csv = dataFrameConverter.convert(mockDataFrame, "csv").get
        val values = csv.split("\n").map(_.split(","))
        values(0) should contain allOf ("foo","bar")
      }
      it("should convert to html") {
        doReturn(rowsOfStrings).when(mockRdd).take(anyInt())
        val html = dataFrameConverter.convert(mockDataFrame, "html").get
        html.contains("<th>foo</th>") should be(true)
        html.contains("<th>bar</th>") should be(true)
      }
      it("should convert limit the selection") {
        doReturn(rowsOfString).when(mockRdd).take(1)
        val someLimited = dataFrameConverter.convert(mockDataFrame, "csv", 1)
        val limitedLines = someLimited.get.split("\n")
        limitedLines.length should be(2)
      }
      it("should return a Failure for invalid types") {
        val result = dataFrameConverter.convert(mockDataFrame, "Invalid Type")
        result.isFailure should be(true)
      }
    }
  }
}
