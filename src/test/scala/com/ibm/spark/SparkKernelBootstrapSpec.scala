package com.ibm.spark

import com.ibm.spark.interpreter.Interpreter
import com.typesafe.config.Config
import org.apache.spark.{SparkContext, SparkConf}
import org.mockito.ArgumentCaptor
import org.scalatest.{Matchers, FunSpec}
import org.mockito.Mockito._
import org.mockito.Matchers._
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
        bootstrap.sparkContext = mock[SparkContext] // Stub out addJar call

        //  Verification
        bootstrap.initializeSparkContext()
        verify(bootstrap).reallyInitializeSparkContext(captor.capture())
        captor.getValue().get("spark.master") should be(expectedVal)
      }

      it("should not add ourselves as a jar if spark.master is not local") {
        val config = mock[Config]
        val sparkMaster: String = "local[*]"
        val bootstrap = spy(new SparkKernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        val mockSparkContext = mock[SparkContext]
        when(config.getString("spark.master")).thenReturn(sparkMaster)
        bootstrap.interpreter = mock[Interpreter]
        bootstrap.sparkContext = mockSparkContext

        //  Verification
        bootstrap.initializeSparkContext()
        bootstrap.reallyInitializeSparkContext(captor.capture())
        verify(mockSparkContext, never()).addJar(anyString())
      }

      it("should add ourselves as a jar if spark.master is not local") {
        val config = mock[Config]
        val sparkMaster: String = "notlocal"
        val bootstrap = spy(new SparkKernelBootstrap(config))
        val captor = ArgumentCaptor.forClass(classOf[SparkConf])

        //  Mocking
        val mockSparkContext = mock[SparkContext]
        when(config.getString("spark.master")).thenReturn(sparkMaster)
        bootstrap.interpreter = mock[Interpreter]
        bootstrap.sparkContext = mockSparkContext

        //  Verification
        val expected =
          com.ibm.spark.SparkKernel.getClass.getProtectionDomain
            .getCodeSource.getLocation.getPath
        bootstrap.initializeSparkContext()
        bootstrap.reallyInitializeSparkContext(captor.capture())
        verify(mockSparkContext, times(2)).addJar(expected)
      }
    }
  }
}
