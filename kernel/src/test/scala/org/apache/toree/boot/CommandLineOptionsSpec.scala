/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.boot

import java.io.File

import com.typesafe.config.Config
import joptsimple.OptionException
import org.scalatest.{FunSpec, Matchers}

import scala.collection.JavaConverters._

class CommandLineOptionsSpec extends FunSpec with Matchers {

  describe("CommandLineOptions") {
    describe("when received --max-interpreter-threads=<int>") {
      it("should set the configuration to the specified value") {
        val expected = 999
        val options = new CommandLineOptions(
          s"--max-interpreter-threads=$expected" :: Nil
        )

        val actual = options.toConfig.getInt("max_interpreter_threads")

        actual should be(expected)
      }
    }

    describe("when received --help") {
      it("should set the help flag to true") {
        val options = new CommandLineOptions("--help" :: Nil)

        options.help should be(true)
      }
    }

    describe("when received -h") {
      it("should set the help flag to true") {
        val options = new CommandLineOptions("-h" :: Nil)

        options.help should be(true)
      }
    }

    describe("when not received --help or -h") {
      it("should set the help flag to false") {
        val options = new CommandLineOptions(Nil)

        options.help should be(false)
      }
    }

    describe("when received --version") {
      it("should set the version flag to true") {
        val options = new CommandLineOptions("--version" :: Nil)

        options.version should be(true)
      }
    }

    describe("when received -v") {
      it("should set the version flag to true") {
        val options = new CommandLineOptions("-v" :: Nil)

        options.version should be(true)
      }
    }

    describe("when not received --version or -v") {
      it("should set the version flag to false") {
        val options = new CommandLineOptions(Nil)

        options.version should be(false)
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
          val options = new CommandLineOptions(Seq("--profile=" + pathToProfileFixture))

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

    describe("when received --<protocol port name>=<value>") {
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

    describe("when received --profile and --<protocol port name>=<value>") {
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

    describe("when no arguments are received") {
      describe("#toConfig") {
        it("should read default value set in reference.conf") {

          val options = new CommandLineOptions(Nil)

          val config: Config = options.toConfig
          config.getInt("stdin_port") should be(48691)
          config.getInt("shell_port") should be(40544)
          config.getInt("iopub_port") should be(43462)
          config.getInt("control_port") should be(44808)
          config.getInt("max_interpreter_threads") should be(4)
          config.getInt("spark_context_initialization_timeout") should be(100)
        }
      }
    }

    describe("when using -- to separate interpreter arguments") {
      describe("#toConfig") {
        it("should return interpreter_args config property when there are args before --") {

          val options = new CommandLineOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be(List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property when args is at the beginning") {

          val options = new CommandLineOptions(List("--", "someArg1", "someArg2", "someArg3"))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be(List("someArg1", "someArg2", "someArg3"))
        }

        it("should return interpreter_args config property as empty list when there is nothing after --") {

          val options = new CommandLineOptions(List("--stdin-port", "99999", "--shell-port", "88888", "--"))

          val config: Config = options.toConfig

          config.entrySet() should not be ('empty)
          config.getStringList("interpreter_args").asScala should be('empty)
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

        it("should set ip to 127.0.0.1") {
          val options = new CommandLineOptions(Nil)
          val config: Config = options.toConfig

          config.getString("ip") should be("127.0.0.1")
        }
      }
    }

    describe("when received options with surrounding whitespace") {
      it("should trim whitespace") {
        val url1 = "url1"
        val url2 = "url2"

        val options = new CommandLineOptions(Seq(
          " --magic-url ", s" ${url1}\t",
          "--magic-url", s" \t ${url2} \t"
        ))
        val config: Config = options.toConfig

        config.getList("magic_urls").unwrapped.asScala should
          be(Seq(url1, url2))
      }
    }

    describe("when received --interpreter-plugin") {
      it("should return the interpreter-plugin along with the defaults") {
        val options = new CommandLineOptions(Seq(
          "--interpreter-plugin",
          "dummy:test.utils.DummyInterpreter"
        ))

        val config: Config = options.toConfig

        val p = config.getList("interpreter_plugins")

        p should not be empty

      }
    }

    describe("when dealing with --spark-context-initialization-timeout") {
      val key = "spark_context_initialization_timeout"

      it("when none of the options are specified, it should default to 100") {
        val options = new CommandLineOptions(Nil)
        val config: Config = options.toConfig
        config.getInt(key) should be(100)
      }

      it("when other options are specified, it should default to 100") {
        val options = new CommandLineOptions(Seq(
          "--interpreter-plugin",
          "dummy:test.utils.DummyInterpreter"
        ))
        val config: Config = options.toConfig
        config.getInt(key) should be(100)
      }

      it("when the options is specified, it should return the specified value") {
        val options = new CommandLineOptions(List(
          "--stdin-port", "99999",
          "--shell-port", "88888",
          "--iopub-port", "77777",
          "--control-port", "55555",
          "--heartbeat-port", "44444",
          "--spark-context-initialization-timeout", "30000"
        ))
        val config: Config = options.toConfig
        config.getInt(key) should be(30000)
      }

      it("when a negative value is specified, it should return the specified value") {
        val options = new CommandLineOptions(List(
          "--stdin-port", "99999",
          "--shell-port", "88888",
          "--iopub-port", "77777",
          "--control-port", "55555",
          "--heartbeat-port", "44444",
          "--spark-context-initialization-timeout", "-1"
        ))
        val config: Config = options.toConfig
        config.getInt(key) should be(-1)
      }

      it("when an invalid value is specified, an exception must be thrown") {
        intercept [OptionException] {
          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444",
            "--spark-context-initialization-timeout", "foo"
          ))
          val config: Config = options.toConfig
        }
      }

      it("when a value is not specified, an exception must be thrown") {
        intercept [OptionException] {
          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444",
            "--spark-context-initialization-timeout", ""
          ))
          val config: Config = options.toConfig
        }
      }
    }

    describe("when dealing with --spark-context-initialization-mode") {
      val key = "spark_context_initialization_mode"

      it("when none of the options are specified, it should default to lazy") {
        val options = new CommandLineOptions(Nil)
        val config: Config = options.toConfig
        config.getString(key) should be("lazy")
      }

      it("when other options are specified, it should default to lazy") {
        val options = new CommandLineOptions(Seq(
          "--interpreter-plugin",
          "dummy:test.utils.DummyInterpreter"
        ))
        val config: Config = options.toConfig
        config.getString(key) should be("lazy")
      }

      it("when the options is specified, it should return the specified value") {
        val options = new CommandLineOptions(List(
          "--stdin-port", "99999",
          "--shell-port", "88888",
          "--iopub-port", "77777",
          "--control-port", "55555",
          "--heartbeat-port", "44444",
          "--spark-context-initialization-mode", "eager"
        ))
        val config: Config = options.toConfig
        config.getString(key) should be("eager")
      }

      it("when the option nosparkcontext is specified, it should properly set spark-context-initialization-mode to none") {
          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444",
            "--nosparkcontext", "foo"
          ))
          val config: Config = options.toConfig
          config.getString(key) should be("none")
        }

        it("when an invalid value is specified, an exception must be thrown") {
        intercept [OptionException] {
          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444",
            "--spark-context-initialization-mode", "foo"
          ))
          val config: Config = options.toConfig
        }
      }

      it("when a value is not specified, an exception must be thrown") {
        intercept [OptionException] {
          val options = new CommandLineOptions(List(
            "--stdin-port", "99999",
            "--shell-port", "88888",
            "--iopub-port", "77777",
            "--control-port", "55555",
            "--heartbeat-port", "44444",
            "--spark-context-initialization-mode", ""
          ))
          val config: Config = options.toConfig
        }
      }
    }

    describe("when dealing with --alternate-sigint") {
      val key = "alternate_sigint"

      it("when option is not specified, Config.hasPath method must return false") {
        val options = new CommandLineOptions(Nil)
        val config: Config = options.toConfig
        config.hasPath(key) should be(false)
      }

      it("when option is specified, the value must be returned") {
        val options = new CommandLineOptions(List(
          "--alternate-sigint", "foo"
        ))
        val config: Config = options.toConfig
        config.getString(key) should be("foo")
      }

      it("when a value is not specified, an exception must be thrown") {
        intercept[OptionException] {
          val options = new CommandLineOptions(List(
            "--alternate-sigint"
          ))
          val config: Config = options.toConfig
        }
      }

    }

  }
}
