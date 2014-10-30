package com.ibm.spark.kernel.protocol.v5

import org.scalatest.{Matchers, FunSpec}

class HeaderBuilderSpec extends FunSpec with Matchers {
  describe("HeaderBuilder") {
    describe("#create") {
      it("should set the msg_id field to a unique ID if none is provided") {
        HeaderBuilder.create("").msg_id should not be empty
      }

      it("should set the msg_id field to the argument if provided") {
        val expected = "some id"
        HeaderBuilder.create("", expected).msg_id should be (expected)
      }

      it("should set the username field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.username
        HeaderBuilder.create("").username should be (expected)
      }

      it("should set the session field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.session
        HeaderBuilder.create("").session should be (expected)
      }

      it("should set the msg_type field to the argument provided") {
        val expected = "some type"
        HeaderBuilder.create(expected).msg_type should be (expected)
      }

      it("should set the version field to the SparkKernelInfo property") {
        val expected = SparkKernelInfo.protocolVersion
        HeaderBuilder.create("").version should be (expected)
      }
    }
  }
}
