package com.ibm.spark.utils

import com.ibm.spark.utils.json.RddToJson
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SchemaRDD, StructType}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsArray, JsString, Json}

class RddToJsonSpec extends FunSpec with MockitoSugar with Matchers {

  val mockSchemaRdd = mock[SchemaRDD]
  val mockRdd = mock[RDD[Any]]
  val mockStruct = mock[StructType]
  val columns = Seq("foo", "bar")
  val rows = Array( Array("a", "b"), Array("c", "d") )

  doReturn(mockStruct).when(mockSchemaRdd).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockSchemaRdd).map(any())(any())
  doReturn(rows).when(mockRdd).take(anyInt())

  describe("RddToJson") {
    describe("#convert(SchemaRDD)") {
      it("should convert to valid JSON object") {

        val json = RddToJson.convert(mockSchemaRdd)
        val jsValue = Json.parse(json)

        jsValue \ "columns" should be (JsArray(Seq(JsString("foo"), JsString("bar"))))
        jsValue \ "rows" should be (JsArray(Seq(
          JsArray(Seq(JsString("a"), JsString("b"))),
          JsArray(Seq(JsString("c"), JsString("d"))))))
      }
    }
  }
}
