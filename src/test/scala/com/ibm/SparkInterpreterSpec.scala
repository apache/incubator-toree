package com.ibm

import org.scalatest.{FunSpec, Matchers}

class SparkInterpreterSpec extends FunSpec with Matchers {

  describe("SparkInterpreter") {
    describe("#interpret") {
      it("should call interpret from the internal SparkIMain") {

      }
    }

    describe("#start") {
      it("should create and bind a Spark context") {

      }

      it("should import everything in org.apache.spark.SparkContext") {

      }
    }

    describe("#stop") {
      it("should stop the internal Spark context") {

      }
    }
  }
}
