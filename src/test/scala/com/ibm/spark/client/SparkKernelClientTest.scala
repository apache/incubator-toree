package com.ibm.spark.client

import com.ibm.spark.kernel.protocol.v5.SocketType
import com.ibm.spark.kernel.protocol.v5.socket.HeartbeatMessage
import org.scalatest.{FunSpec, Matchers}

/**
 * Created by dalogsdon on 8/5/14.
 */
class SparkKernelClientTest extends FunSpec with Matchers {

  describe("test client") {
    it("should send heartbeat message") {
      val client = new SparkKernelClient()
      val actorSelection = client.actorLoader.load(SocketType.HeartbeatClient)
      actorSelection ! HeartbeatMessage
      Thread.sleep(10*1000)
    }
  }
}
