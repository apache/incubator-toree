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

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.{JsArray, JsString, Json}

class RddToJsonSpec extends FunSpec with MockitoSugar with Matchers {

  val mockDataFrame = mock[DataFrame]
  val mockRdd = mock[RDD[Any]]
  val mockStruct = mock[StructType]
  val columns = Seq("foo", "bar").toArray
  val rows = Array( Array("a", "b"), Array("c", "d") )

  doReturn(mockStruct).when(mockDataFrame).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockDataFrame).map(any())(any())
  doReturn(rows).when(mockRdd).take(anyInt())

  describe("RddToJson") {
    describe("#convert(SchemaRDD)") {
      it("should convert to valid JSON object") {

        val json = RddToJson.convert(mockDataFrame)
        val jsValue = Json.parse(json)

        jsValue \ "columns" should be (JsArray(Seq(JsString("foo"), JsString("bar"))))
        jsValue \ "rows" should be (JsArray(Seq(
          JsArray(Seq(JsString("a"), JsString("b"))),
          JsArray(Seq(JsString("c"), JsString("d"))))))
      }
    }
  }
}
