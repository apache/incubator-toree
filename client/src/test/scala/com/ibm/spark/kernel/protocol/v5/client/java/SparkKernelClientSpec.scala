package com.ibm.spark.client.java

import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import scala.runtime.BoxedUnit
import com.ibm.spark.kernel.protocol.v5.client.java.{EmptyFunction, SparkKernelClient}

class SparkKernelClientSpec extends FunSpec with Matchers with MockitoSugar
  with BeforeAndAfter
{
  private var sparkKernelClient: SparkKernelClient = _
  private var mockScalaClient: com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient = _

  before {
    mockScalaClient = mock[com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient]
    sparkKernelClient = new SparkKernelClient(mockScalaClient)
  }

  describe("[Java] SparkKernelClient") {
    describe("#heartbeat") {
      it("should execute the failure callback on failure") {
        // Mock the callbacks
        val mockFailure = mock[EmptyFunction]

        sparkKernelClient.heartbeat(mockFailure)

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        val failureCaptor = ArgumentCaptor.forClass(classOf[() => Unit])
        verify(mockScalaClient).heartbeat(failureCaptor.capture())

        // Invoke the failure, which mocks a client error
        failureCaptor.getValue.apply()

        // Verify failure was called and success was not
        verify(mockFailure).invoke()
      }
    }

    describe("#submit") {
      it("should invoke the underlying Scala SparkKernelClient implementation") {
        sparkKernelClient.execute("foo code")
        verify(mockScalaClient).execute(anyString())
      }
    }

    describe("#stream") {
      it("should invoke the underlying Scala SparkKernelClient implementation") {
        val func = mock[(AnyRef) => BoxedUnit]
        sparkKernelClient.execute("bar code")
        verify(mockScalaClient).execute(anyString())
      }
    }
  }
}
