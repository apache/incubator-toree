package com.ibm.spark.magic.builtin

import com.ibm.spark.interpreter.{ExecuteAborted, ExecuteError, Interpreter}
import com.ibm.spark.interpreter.Results.Result
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.dependencies.IncludeInterpreter
import org.apache.spark.sql.{StructType, SchemaRDD}
import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.libs.json.Json

class RDDSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val resOutput = "res1: org.apache.spark.sql.SchemaRDD ="

  val mockInterpreter = mock[Interpreter]
  val mockSchemaRdd = mock[SchemaRDD]
  val mockRdd = mock[org.apache.spark.rdd.RDD[Any]]
  val mockStruct = mock[StructType]
  val columns = Seq("foo", "bar")
  val rows = Array( Array("a", "b"), Array("c", "d") )

  doReturn(mockStruct).when(mockSchemaRdd).schema
  doReturn(columns).when(mockStruct).fieldNames
  doReturn(mockRdd).when(mockSchemaRdd).map(any())(any())
  doReturn(rows).when(mockRdd).take(anyInt())

  val rddMagic = new RDD with IncludeInterpreter {
    override val interpreter: Interpreter = mockInterpreter
  }

  before {
    doReturn(Some(mockSchemaRdd)).when(mockInterpreter).read(anyString())
    doReturn((mock[Result], Left(resOutput))).when(mockInterpreter).interpret(anyString(), anyBoolean())
  }

  describe("RDD") {
    describe("#executeCell") {
      it("should return valid JSON when the executed code evaluates to a SchemaRDD") {
        val magicOutput = rddMagic.executeCell(Seq("schemaRDD"))
        magicOutput.contains(MIMEType.ApplicationJson) should be (true)
        Json.parse(magicOutput(MIMEType.ApplicationJson))
      }
      it("should return normally when the executed code does not evaluate to a SchemaRDD") {
        doReturn((mock[Result], Left("foo"))).when(mockInterpreter).interpret(anyString(), anyBoolean())
        val magicOutput = rddMagic.executeCell(Seq(""))
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }
      it("should return error message when the interpreter does not return SchemaRDD as expected") {
        doReturn(Some("foo")).when(mockInterpreter).read(anyString())
        val magicOutput = rddMagic.executeCell(Seq(""))
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }
      it("should throw a Throwable if the interpreter returns an ExecuteError") {
        val expected = "some error message"
        val mockExecuteError = mock[ExecuteError]
        doReturn(expected).when(mockExecuteError).value

        doReturn((mock[Result], Right(mockExecuteError))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.executeCell(Seq(""))
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }
      it("should throw a Throwable if the interpreter returns an ExecuteAborted") {
        val expected = "RDD magic aborted!"
        val mockExecuteAborted = mock[ExecuteAborted]

        doReturn((mock[Result], Right(mockExecuteAborted))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.executeCell(Seq(""))
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }
    }

    describe("#executeLine") {
      it("should return valid JSON when the executed code evaluates to a SchemaRDD") {
        val magicOutput = rddMagic.executeLine("schemaRDD")
        magicOutput.contains(MIMEType.ApplicationJson) should be (true)
        Json.parse(magicOutput(MIMEType.ApplicationJson))
      }
      it("should return normally when the executed code does not evaluate to a SchemaRDD") {
        doReturn((mock[Result], Left("foo"))).when(mockInterpreter).interpret(anyString(), anyBoolean())
        val magicOutput = rddMagic.executeLine("")
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }
      it("should return error message when the interpreter does not return SchemaRDD as expected") {
        doReturn(Some("foo")).when(mockInterpreter).read(anyString())
        val magicOutput = rddMagic.executeLine("")
        magicOutput.contains(MIMEType.PlainText) should be (true)
      }
      it("should throw a Throwable if the interpreter returns an ExecuteError") {
        val expected = "some error message"
        val mockExecuteError = mock[ExecuteError]
        doReturn(expected).when(mockExecuteError).value

        doReturn((mock[Result], Right(mockExecuteError))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.executeLine("")
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }
      it("should throw a Throwable if the interpreter returns an ExecuteAborted") {
        val expected = "RDD magic aborted!"
        val mockExecuteAborted = mock[ExecuteAborted]

        doReturn((mock[Result], Right(mockExecuteAborted))).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())
        val actual = {
          val exception = intercept[Throwable] {
            rddMagic.executeLine("")
          }
          exception.getLocalizedMessage
        }

        actual should be (expected)
      }
    }
  }
}