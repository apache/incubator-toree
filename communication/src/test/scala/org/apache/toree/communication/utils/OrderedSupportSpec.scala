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

package org.apache.toree.communication.utils

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}

case class OrderedType()
case class NotOrderedType()
case class FinishProcessingMessage()
case class ReceiveMessageCount(count: Int)

class TestOrderedSupport extends OrderedSupport {
  var receivedCounter = 0
  override def orderedTypes(): Seq[Class[_]] = Seq(classOf[OrderedType])

  override def receive: Receive = {
    case OrderedType() =>
      startProcessing()
      receivedCounter = receivedCounter + 1
      sender ! ReceiveMessageCount(receivedCounter)
    case NotOrderedType() =>
      receivedCounter = receivedCounter + 1
      sender ! ReceiveMessageCount(receivedCounter)
    case FinishProcessingMessage() =>
      finishedProcessing()
  }
}

class OrderedSupportSpec extends TestKit(ActorSystem("OrderedSupportSystem"))
  with ImplicitSender with Matchers with FunSpecLike
  with MockitoSugar  {

  describe("OrderedSupport"){
    describe("#waiting"){
      it("should wait for types defined in orderedTypes"){
      val testOrderedSupport = system.actorOf(Props[TestOrderedSupport])

        // Send a message having a type in orderedTypes
        // Starts processing and is handled with receive()
        testOrderedSupport ! new OrderedType
        // This message should be handled with waiting()
        testOrderedSupport ! new OrderedType

        // Verify receive was not called for the second OrderedType
        expectMsg(ReceiveMessageCount(1))

      }

      it("should process types not defined in orderedTypes"){
        val testOrderedSupport = system.actorOf(Props[TestOrderedSupport])

        // Send a message that starts the processing
        testOrderedSupport ! new OrderedType

        // Send a message having a type not in orderedTypes
        testOrderedSupport ! new NotOrderedType

        // Verify receive did get called for NotOrderedType
        expectMsg(ReceiveMessageCount(1))
        expectMsg(ReceiveMessageCount(2))
      }
    }
    describe("#finishedProcessing"){
      it("should switch actor to receive method"){
        val testOrderedSupport = system.actorOf(Props[TestOrderedSupport])
        
        //  Switch actor to waiting mode
        testOrderedSupport ! new OrderedType

        //  Call finishedProcessing
        testOrderedSupport ! new FinishProcessingMessage

        //  Sending something that would match in receive, and is in orderedTypes
        testOrderedSupport ! new OrderedType

        expectMsg(ReceiveMessageCount(1))
        expectMsg(ReceiveMessageCount(2))

      }

    }
  }

}
