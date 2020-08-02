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



/*
package integration.socket

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.apache.toree.kernel.protocol.v5.client.ZMQMessage
import org.apache.toree.kernel.protocol.v5.SocketType
import org.apache.toree.kernel.protocol.v5.socket._
import org.apache.toree.kernel.protocol.v5.socket.SocketConfig
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import scala.concurrent.duration._


class ClientToHeartbeatSpecForIntegration extends TestKit(ActorSystem("HeartbeatActorSpec"))
  with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {

  describe("HeartbeatActor") {
    implicit val timeout = Timeout(1.minute)
    val clientSocketFactory = mock[ClientSocketFactory]
    val serverSocketFactory = mock[ServerSocketFactory]
    val probe: TestProbe = TestProbe()
    val probeClient: TestProbe = TestProbe()
    when(serverSocketFactory.Heartbeat(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probe.ref)
    when(clientSocketFactory.HeartbeatClient(any(classOf[ActorSystem]), any(classOf[ActorRef]))).thenReturn(probeClient.ref)

    val heartbeat = system.actorOf(Props(classOf[Heartbeat], serverSocketFactory))
    val heartbeatClient = system.actorOf(Props(classOf[HeartbeatClient], clientSocketFactory))

    describe("send heartbeat") {
      it("should send and receive same ZMQMessage") {
        heartbeatClient ? HeartbeatMessage
        probeClient.expectMsgClass(classOf[ZMQMessage])
        probeClient.forward(heartbeat)
        probe.expectMsgClass(classOf[ZMQMessage])
        probe.forward(heartbeatClient)
      }
    }

    describe("send heartbeat") {
      it("should work with real actorsystem and no probes") {
        val system = ActorSystem("iopubtest")

        val socketConfig = SocketConfig.fromConfig(ConfigFactory.parseString(
          """
            {
                "stdin_port": 8000,
                "ip": "127.0.0.1",
                "control_port": 8001,
                "hb_port": 8002,
                "signature_scheme": "hmac-sha256",
                "key": "",
                "shell_port": 8003,
                "transport": "tcp",
                "iopub_port": 8004
            }
          """.stripMargin)
        )
        val clientSocketFactory = new ClientSocketFactory(socketConfig)
        val ioPUB = system.actorOf(Props(classOf[ActorRef], serverSocketFactory), name = SocketType.IOPub.toString)
      }
    }
  }
}
*/