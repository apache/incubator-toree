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

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.{JsArray, JsDefined, JsString, JsValue, Json}
import test.utils.SparkContextProvider

import scala.collection.mutable

class DataFrameConverterSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll {

  lazy val spark = SparkContextProvider.sparkContext

  override protected def afterAll(): Unit = {
    spark.stop()
    super.afterAll()
  }

  val dataFrameConverter: DataFrameConverter = new DataFrameConverter
  val mockDataFrame = mock[DataFrame]
  val mockRdd = spark.parallelize(Seq(Row(new mutable.WrappedArray.ofRef(Array("test1", "test2")), 2, null)))
  val mockStruct = mock[StructType]
  val columns = Array("foo", "bar")

  doReturn(mockStruct).when(mockDataFrame).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockDataFrame).rdd

  describe("DataFrameConverter") {
    describe("#convert") {
      it("should convert to a valid JSON object") {
        val someJson = dataFrameConverter.convert(mockDataFrame, "json")
        val jsValue = Json.parse(someJson.get)
        (jsValue \ "columns").as[Array[JsValue]] should contain theSameElementsAs Array(JsString("foo"), JsString("bar"))
        (jsValue \ "rows").as[Array[JsValue]] should contain theSameElementsAs Array(
          JsArray(Seq(JsString("[test1, test2]"), JsString("2"), JsString("null")))
        )
      }
      it("should convert to csv") {
        val csv = dataFrameConverter.convert(mockDataFrame, "csv").get
        val values = csv.split("\n")
        values(0) shouldBe "foo,bar"
        values(1) shouldBe "[test1, test2],2,null"
      }
      it("should convert to html") {
        val html = dataFrameConverter.convert(mockDataFrame, "html").get
        html.contains("<th>foo</th>") should be(true)
        html.contains("<th>bar</th>") should be(true)
        html.contains("<td>[test1, test2]</td>") should be(true)
        html.contains("<td>2</td>") should be(true)
        html.contains("<td>null</td>") should be(true)
      }
      it("should convert limit the selection") {
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
