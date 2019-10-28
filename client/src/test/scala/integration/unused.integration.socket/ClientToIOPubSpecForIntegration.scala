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

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.content.ExecuteResult
import org.apache.toree.kernel.protocol.v5.socket._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

class ClientToIOPubSpecForIntegration extends TestKit(ActorSystem("IOPubSystem"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("Client-IOPub Integration"){
    describe("Client"){
      it("should connect to IOPub Socket"){
        // setup
        val mockClientSocketFactory = mock[ClientSocketFactory]
        val mockServerSocketFactory = mock[ServerSocketFactory]
        val iopubProbe = TestProbe()
        when(mockServerSocketFactory.IOPub(any[ActorSystem]))
          .thenReturn(iopubProbe.ref)

        // Server and client sockets
        val ioPub = system.actorOf(
          Props(classOf[IOPub], mockServerSocketFactory),
          name = SocketType.IOPub.toString
        )

        val ioPubClient = system.actorOf(
          Props(classOf[IOPubClient], mockClientSocketFactory),
          name = SocketType.IOPubClient.toString
        )

        // Give some buffer for the server socket to be bound
        Thread.sleep(100)

        // Register ourselves as sender in IOPubClient's sender map
        val id = UUID.randomUUID().toString
        ioPubClient ! id

        // construct the message and send it
        val result = ExecuteResult(1, Data(), Metadata())
        val header = Header(UUID.randomUUID().toString, "spark", UUID.randomUUID().toString, MessageType.ExecuteResult.toString, "5.0")
        val parentHeader = Header(id, "spark", UUID.randomUUID().toString, MessageType.ExecuteRequest.toString, "5.0")
        val kernelMessage = new KernelMessage(
          Seq[String](), "",
          header, parentHeader,
          Metadata(), Json.toJson(result).toString()
        )

        // Send the message on the IOPub server socket
        ioPub ! kernelMessage
        val message = iopubProbe.receiveOne(2000.seconds)
        iopubProbe.forward(ioPubClient, message)

        // ioPubClient should have received the message
        expectMsg(result)
      }
    }
  }
}
*/