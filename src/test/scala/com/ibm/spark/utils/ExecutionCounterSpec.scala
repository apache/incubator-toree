package com.ibm.spark.utils

import org.scalatest.{Matchers, FunSpec}

class ExecutionCounterSpec extends FunSpec with Matchers {
  describe("ExecutionCounter") {
    describe("#increment( String )"){
      it("should increment value when key is not present"){
        ExecutionCounter incr "foo" should be(1)
      }
      it("should increment value for key when it is present"){
        ExecutionCounter incr "bar" should be(1)
        ExecutionCounter incr "bar" should be(2)
      }

    }
  }
}
