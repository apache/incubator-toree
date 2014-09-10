package com.ibm.spark

import java.io.File

import com.typesafe.config.Config
import joptsimple.OptionException
import org.scalatest.{FunSpec, Matchers}

import scala.collection.JavaConverters._

class SparkKernelOptionsSpec extends FunSpec with Matchers {

  describe("SparkKernelOptions") {
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

    describe("when received --master=<value>") {
      it("should error if value is not set") {
        try {
          new SparkKernelOptions(Seq("--master"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
      }

      describe("#toConfig") {
        it("should set master to specified value") {
          val expected = "test"
          val options = new SparkKernelOptions(s"--master=${expected}" :: Nil)
          val config: Config = options.toConfig

          config.getString("spark.master") should be(expected)
        }

        it("should set master to local[*]") {
          val options = new SparkKernelOptions(Nil)
          val config: Config = options.toConfig

          config.getString("spark.master") should be("local[*]")
        }
      }
    }

    describe("when received --profile=<path>") {
      it("should error if path is not set") {
        try {
          new SparkKernelOptions(Seq("--profile"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
      }

      describe("#toConfig") {
        it("should include values specified in file") {

          val pathToProfileFixture: String = (new File(getClass.getResource("/fixtures/profile.json").toURI)).getAbsolutePath
          val options = new SparkKernelOptions(Seq("--profile="+pathToProfileFixture))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getInt("stdin_port") should be(12345)
          config.getInt("shell_port") should be(54321)
          config.getInt("iopub_port") should be(11111)
          config.getInt("control_port") should be(22222)
        }
      }
    }

    describe("when received --<protocol port name>=<value>"){
      it("should error if value is not set") {
        try {
          new SparkKernelOptions(Seq("--stdin-port"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
        try{
          new SparkKernelOptions(Seq("--shell-port"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
        try{
          new SparkKernelOptions(Seq("--iopub-port"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
        try {
          new SparkKernelOptions(Seq("--control-port"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
        try{
          new SparkKernelOptions(Seq("--heartbeat-port"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
      }

      describe("#toConfig") {
        it("should return config with commandline option values") {

          val options = new SparkKernelOptions(List("--stdin-port", "99999", "--shell-port", "88888"))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getInt("stdin_port") should be(99999)
          config.getInt("shell_port") should be(88888)
          config.getInt("iopub_port") should be(43462)
        }
      }
    }

    describe("when received --profile and --<protocol port name>=<value>"){
      describe("#toConfig") {
        it("should return config with <protocol port> argument value") {

          val pathToProfileFixture: String = (new File(getClass.getResource("/fixtures/profile.json").toURI)).getAbsolutePath
          val options = new SparkKernelOptions(List("--profile", pathToProfileFixture, "--stdin-port", "99999", "--shell-port", "88888"))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getInt("stdin_port") should be(99999)
          config.getInt("shell_port") should be(88888)
          config.getInt("iopub_port") should be(11111)
          config.getInt("control_port") should be(22222)
        }
      }

    }

    describe("when no arguments are received"){
      describe("#toConfig") {
        it("should read default value set in reference.conf") {

          val options = new SparkKernelOptions(Nil)

          val config: Config = options.toConfig
          config.getInt("stdin_port") should be(48691)
          config.getInt("shell_port") should be(40544)
          config.getInt("iopub_port") should be(43462)
          config.getInt("control_port") should be(44808)
        }
      }
    }

    describe("when using -- to separate interpreter arguments"){
      describe("#toConfig") {
        it("should return interpreter_args config property when there are args before --") {

          val options = new SparkKernelOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be (List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property when args is at the beginning") {

          val options = new SparkKernelOptions(List("--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be (List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property as empty list when there is nothing after --") {

          val options = new SparkKernelOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be ('empty)
        }
      }
    }

    describe("when received --ip=<value>") {
      it("should error if value is not set") {
        try {
          new SparkKernelOptions(Seq("--ip"))
          fail("Expected a parse error.")
        } catch {
          case e: OptionException =>
        }
      }

      describe("#toConfig") {
        it("should set ip to specified value") {
          val expected = "1.2.3.4"
          val options = new SparkKernelOptions(s"--ip=${expected}" :: Nil)
          val config: Config = options.toConfig

          config.getString("ip") should be(expected)
        }

        it("should set master to local[*]") {
          val options = new SparkKernelOptions(Nil)
          val config: Config = options.toConfig

          config.getString("ip") should be("127.0.0.1")
        }
      }
    }
  }

}
