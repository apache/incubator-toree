package com.ibm.spark.kernel.protocol.v5.socket

import java.io.File

import org.scalatest.{Matchers, FunSpec}

class SocketConfigReaderSpec extends FunSpec with Matchers {

  val socketConfigReader = new SocketConfigReader(Option[File](new File("src/test/resources/profile.json")))
  val socketConfig = socketConfigReader.getSocketConfig

  describe("SocketConfigReader") {
    describe("read config") {
      it("should convert json file to SocketConfig object") {
        socketConfig.stdin_port should be (8000)
      }
    }
  }
}
