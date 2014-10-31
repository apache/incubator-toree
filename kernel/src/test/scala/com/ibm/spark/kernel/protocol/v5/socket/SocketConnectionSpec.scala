package com.ibm.spark.kernel.protocol.v5.socket

import org.scalatest.{FunSpec, Matchers}

class SocketConnectionSpec extends FunSpec with Matchers {
  describe("SocketConnection"){
   describe("#toString"){
   	it("should properly format connection string"){
      val connection: SocketConnection = SocketConnection("tcp", "127.0.0.1", 1234)
      connection.toString should be ("tcp://127.0.0.1:1234")
   	}
   }
  }

}
