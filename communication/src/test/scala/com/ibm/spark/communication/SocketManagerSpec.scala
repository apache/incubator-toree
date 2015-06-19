package com.ibm.spark.communication

import org.scalatest.{Matchers, FunSpec}
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Context

class SocketManagerSpec extends FunSpec with Matchers {
  describe("SocketManager") {
    val socketManager = new SocketManager()
    //socketManager.create(ZMQ.PUB)





    it("playground"){
      println(s"Main Thread: ${Thread.currentThread().getId}")
      new Thread(new Runnable {
        println(s"Constructor: ${Thread.currentThread().getId}")
        override def run(): Unit = {
          println(s"Run: ${Thread.currentThread().getId}")
        }
      }).start()
    }
  }
}
