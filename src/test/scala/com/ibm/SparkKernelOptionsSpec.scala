package com.ibm

import org.scalatest.{Matchers, FunSpec}

class SparkKernelOptionsSpec extends FunSpec with Matchers {

  describe("SparkKernelOptions") {
    describe("when received --profile") {
      it("should set the profile field to an optional file if it is provided") {
        val options = new SparkKernelOptions("--profile" :: "somefile" :: Nil)

        options.profile should not be (None)
      }
    }

    describe("when not received --profile") {
      it("should set the profile field to none") {
        val options = new SparkKernelOptions(Nil)

        options.profile should be (None)
      }
    }

    describe("when received --create-context") {
      it("should set the create_context flag to true if --create-context=true") {
        val options = new SparkKernelOptions("--create-context=true" :: Nil)

        options.create_context should be (true)
      }

      it("should set the create_context flag to false if --create-context=false") {
        val options = new SparkKernelOptions("--create-context=false" :: Nil)

        options.create_context should be (false)
      }
    }

    describe("when not received --create-context") {
      it("should set the create_context flag to true") {
        val options = new SparkKernelOptions(Nil)

        options.create_context should be (true)
      }
    }

    describe("when received --help") {
      it("should report the help message and exit") {
        val options = new SparkKernelOptions("--help" :: Nil)

        options.help should be (true)
      }
    }

    describe("when not received --help") {
      it("should set the help flag to false") {
        val options = new SparkKernelOptions(Nil)

        options.help should be (false)
      }
    }

    describe("when received --verbose") {
      it("should set the verbose flag to true") {
        val options = new SparkKernelOptions("--verbose" :: Nil)

        options.verbose should be (true)
      }
    }

    describe("when not received --verbose") {
      it("should set the verbose flag to false") {
        val options = new SparkKernelOptions(Nil)

        options.verbose should be (false)
      }
    }
  }
}
