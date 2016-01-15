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

package org.apache.toree.kernel.protocol.v5.kernel.socket

import org.scalatest.{FunSpec, Matchers}

class SocketFactorySpec extends FunSpec with Matchers {
  describe("SocketFactory"){
    describe("HeartbeatConnection"){
    	it("should be composed of transport ip and heartbeat port"){
        val config: SocketConfig = SocketConfig(-1,-1,8000,-1, -1, "<STRING-IP>", "<STRING-TRANSPORT>","<STRING-SCHEME>","<STRING-KEY>")
        val factory: SocketFactory = SocketFactory(config)
        factory.HeartbeatConnection.toString should be ("<STRING-TRANSPORT>://<STRING-IP>:8000")
    	}
    }
  }
}
