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

package org.apache.toree.utils

import org.apache.toree.kernel.protocol.v5.{MessageType, KernelMessage}

trait MessageLogSupport extends LogLike {
  /**
   * Logs various pieces of a KernelMessage at different levels of logging.
   * @param km
   */
  def logMessage(km: KernelMessage): Unit = {
    logger.trace(s"Kernel message ids: ${km.ids}")
    logger.trace(s"Kernel message signature: ${km.signature}")
    logger.debug(s"Kernel message header id: ${km.header.msg_id}")
    logger.debug(s"Kernel message header type: ${km.header.msg_type}")
    val incomingMessage = isIncomingMessage(km.header.msg_type)
    (km.parentHeader, incomingMessage) match {
      case (null, true)   =>  //  Don't do anything, this is expected
      case (null, false)  =>  //  Messages coming from the kernel should have parent headers
        logger.warn(s"Parent header is null for message ${km.header.msg_id} " +
          s"of type ${km.header.msg_type}")
      case _ =>
        logger.trace(s"Kernel message parent id: ${km.parentHeader.msg_id}")
        logger.trace(s"Kernel message parent type: ${km.parentHeader.msg_type}")
    }
    logger.trace(s"Kernel message metadata: ${km.metadata}")
    logger.trace(s"Kernel message content: ${km.contentString}")
  }

  /**
   * Logs an action, along with message id and type for a KernelMessage.
   * @param action
   * @param km
   */
  def logKernelMessageAction(action: String, km: KernelMessage): Unit = {
    logger.debug(s"${action} KernelMessage ${km.header.msg_id} " +
      s"of type ${km.header.msg_type}")
  }

  // TODO: Migrate this to a helper method in MessageType.Incoming
  /**
   * This method is used to determine if a message is being received by the
   * kernel or being sent from the kernel.
   * @return true if the message is received by the kernel, false otherwise.
   */
  private def isIncomingMessage(messageType: String): Boolean ={
    MessageType.Incoming.CompleteRequest.toString.equals(messageType) ||
      MessageType.Incoming.ConnectRequest.toString.equals(messageType) ||
      MessageType.Incoming.ExecuteRequest.toString.equals(messageType) ||
      MessageType.Incoming.HistoryRequest.toString.equals(messageType) ||
      MessageType.Incoming.InspectRequest.toString.equals(messageType) ||
      MessageType.Incoming.ShutdownRequest.toString.equals(messageType)||
      MessageType.Incoming.KernelInfoRequest.toString.equals(messageType) ||
      MessageType.Incoming.CommOpen.toString.equals(messageType) ||
      MessageType.Incoming.CommMsg.toString.equals(messageType) ||
      MessageType.Incoming.CommClose.toString.equals(messageType) ||
      MessageType.Incoming.IsCompleteRequest.toString.equals(messageType)
  }

}
