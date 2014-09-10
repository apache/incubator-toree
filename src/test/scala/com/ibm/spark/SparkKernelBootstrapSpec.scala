package com.ibm.spark

import com.ibm.spark.interpreter.Interpreter
import com.typesafe.config.Config
import org.apache.spark.SparkConf
import org.mockito.ArgumentCaptor
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

class SparkKernelBootstrapSpec extends FunSpec with Matchers with MockitoSugar {
  describe("SparkKernelBootstrap") {
    describe("when spark.master is set in config") {
      it("should set spark.master in SparkConf") {
        val config = mock[Config]
        val expectedVal: String = "expected val"
        val bootstrap = spy(new SparkKernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        when(config.getString("spark.master")).thenReturn(expectedVal)
        bootstrap.interpreter = mock[Interpreter]

        //  Verification
        bootstrap.initializeSparkContext()
        verify(bootstrap).reallyInitializeSparkContext(captor.capture())
        captor.getValue().get("spark.master") should be(expectedVal)
      }
    }
  }
}
