/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.boot

import java.io.File

import com.typesafe.config.Config
import joptsimple.OptionException
import org.scalatest.{FunSpec, Matchers}

import scala.collection.JavaConverters._

class CommandLineOptionsSpec extends FunSpec with Matchers {

  describe("CommandLineOptions") {
    describe("when received --help") {
      it("should report the help message and exit") {
        val options = new CommandLineOptions("--help" :: Nil)

        options.help should be (true)
      }
    }

    describe("when not received --help") {
      it("should set the help flag to false") {
        val options = new CommandLineOptions(Nil)

        options.help should be (false)
      }
    }

    describe("when received --master=<value>") {
      it("should error if value is not set") {
        intercept[OptionException] {
          new CommandLineOptions(Seq("--master"))
        }
      }

      describe("#toConfig") {
        it("should set master to specified value") {
          val expected = "test"
          val options = new CommandLineOptions(s"--master=${expected}" :: Nil)
          val config: Config = options.toConfig

          config.getString("spark.master") should be(expected)
        }

        it("should set master to local[*]") {
          val options = new CommandLineOptions(Nil)
          val config: Config = options.toConfig

          config.getString("spark.master") should be("local[*]")
        }
      }
    }

    describe("when received --profile=<path>") {
      it("should error if path is not set") {
        intercept[OptionException] {
          new CommandLineOptions(Seq("--profile"))
        }
      }

      describe("#toConfig") {
        it("should include values specified in file") {

          val pathToProfileFixture: String = new File(getClass.getResource("/fixtures/profile.json").toURI).getAbsolutePath
          val options = new CommandLineOptions(Seq("--profile="+pathToProfileFixture))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getInt("stdin_port") should be(12345)
          config.getInt("shell_port") should be(54321)
          config.getInt("iopub_port") should be(11111)
          config.getInt("control_port") should be(22222)
          config.getInt("hb_port") should be(33333)
        }
      }
    }

    describe("when received --<protocol port name>=<value>"){
      it("should error if value is not set") {
        intercept[OptionException] {
          new CommandLineOptions(Seq("--stdin-port"))
        }
        intercept[OptionException] {
          new CommandLineOptions(Seq("--shell-port"))
        }
        intercept[OptionException] {
          new CommandLineOptions(Seq("--iopub-port"))
        }
        intercept[OptionException] {
          new CommandLineOptions(Seq("--control-port"))
        }
        intercept[OptionException] {
          new CommandLineOptions(Seq("--heartbeat-port"))
        }
      }

      describe("#toConfig") {
        it("should return config with commandline option values") {

          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444"
          ))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getInt("stdin_port") should be(99999)
          config.getInt("shell_port") should be(88888)
          config.getInt("iopub_port") should be(77777)
          config.getInt("control_port") should be(55555)
          config.getInt("hb_port") should be(44444)
        }
      }
    }

    describe("when received --profile and --<protocol port name>=<value>"){
      describe("#toConfig") {
        it("should return config with <protocol port> argument value") {

          val pathToProfileFixture: String = (new File(getClass.getResource("/fixtures/profile.json").toURI)).getAbsolutePath
          val options = new CommandLineOptions(List("--profile", pathToProfileFixture, "--stdin-port", "99999", "--shell-port", "88888"))

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

          val options = new CommandLineOptions(Nil)

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

          val options = new CommandLineOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be (List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property when args is at the beginning") {

          val options = new CommandLineOptions(List("--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be (List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property as empty list when there is nothing after --") {

          val options = new CommandLineOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--"))

          val config: Config = options .toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be ('empty)
        }
      }
    }

    describe("when received --ip=<value>") {
      it("should error if value is not set") {
        intercept[OptionException] {
          new CommandLineOptions(Seq("--ip"))
        }
      }

      describe("#toConfig") {
        it("should set ip to specified value") {
          val expected = "1.2.3.4"
          val options = new CommandLineOptions(s"--ip=${expected}" :: Nil)
          val config: Config = options.toConfig

          config.getString("ip") should be(expected)
        }

        it("should set master to local[*]") {
          val options = new CommandLineOptions(Nil)
          val config: Config = options.toConfig

          config.getString("ip") should be("127.0.0.1")
        }
      }
    }
  }

}
