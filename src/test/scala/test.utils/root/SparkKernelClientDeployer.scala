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

package test.utils.root

import org.apache.toree.kernel.protocol.v5.client.boot.ClientBootstrap
import org.apache.toree.kernel.protocol.v5.client.boot.layers._
import com.typesafe.config.{ConfigFactory, Config}

/**
  * Provides tests with a generic Spark Kernel client.
  */
object SparkKernelClientDeployer {
   lazy val startInstance = {
     val profileJSON: String = """
     {
         "stdin_port":   48691,
         "control_port": 40544,
         "hb_port":      43462,
         "shell_port":   44808,
         "iopub_port":   49691,
         "ip": "127.0.0.1",
         "transport": "tcp",
         "signature_scheme": "hmac-sha256",
         "key": ""
     }
                               """.stripMargin
     val config: Config = ConfigFactory.parseString(profileJSON)
     (new ClientBootstrap(config)
       with StandardSystemInitialization
       with StandardHandlerInitialization).createClient()
   }
 }
