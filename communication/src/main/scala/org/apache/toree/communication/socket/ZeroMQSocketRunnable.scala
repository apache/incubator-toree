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
package org.apache.toree.communication.socket

import org.apache.toree.utils.LogLike
import org.zeromq.{ZMsg, ZMQ}
import org.zeromq.ZMQ.Context

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Represents the runnable component of a socket that processes messages and
 * sends messages placed on an outbound queue.
 *
 * @param context The ZMQ context to use with this runnable to create a socket
 * @param socketType The type of socket to create
 * @param inboundMessageCallback The callback to invoke when receiving a message
 *                               on the socket created
 * @param socketOptions The options to use when creating the socket
 */
class ZeroMQSocketRunnable(
  private val context: Context,
  private val socketType: SocketType,
  private val inboundMessageCallback: Option[(Seq[Array[Byte]]) => Unit],
  private val socketOptions: SocketOption*
) extends SocketRunnable[ZMsg](inboundMessageCallback)
  with LogLike {
  require(socketOptions.count {
    case _: Bind    => true
    case _: Connect => true
    case _          => false
  } == 1, "ZeroMQ socket needs exactly one bind or connect!")

  @volatile private var notClosed: Boolean = true
  @volatile private var _isProcessing: Boolean = false

  /**
   * Indicates the processing state of this runnable.
   *
   * @return True if processing messages, otherwise false
   */
  override def isProcessing: Boolean = _isProcessing

  /**
   * Processes the provided options, performing associated actions on the
   * specified socket.
   *
   * @param socket The socket to apply actions on
   */
  protected def processOptions(socket: ZMQ.Socket): Unit = {
    val socketOptionsString = socketOptions.map("\n- " + _.toString).mkString("")
    logger.trace(
      s"Processing options for socket $socketType: $socketOptionsString"
    )

    // Split our options based on connection (bind/connect) and everything else
    val (connectionOptions, otherOptions) = socketOptions.partition {
      case Bind(_) | Connect(_) => true
      case _ => false
    }

    // Apply non-connection options first since some (like identity) must be
    // run before the socket does a bind/connect
    otherOptions.foreach {
      case Linger(milliseconds) => socket.setLinger(milliseconds)
      case Subscribe(topic)     => socket.subscribe(topic)
      case Identity(identity)   => socket.setIdentity(identity)
      case option               => logger.warn(s"Unknown option: $option")
    }

    // Perform our bind or connect
    connectionOptions.foreach {
      case Bind(address)        => socket.bind(address)
      case Connect(address)     => socket.connect(address)
      case option               =>
        logger.warn(s"Unknown connection option: $option")
    }

    _isProcessing = true
  }

  /**
   * Sends the next outbound message from the outbound message queue.
   *
   * @param socket The socket to use when sending the message
   *
   * @return True if a message was sent, otherwise false
   */
  protected def processNextOutboundMessage(socket: ZMQ.Socket): Boolean = {
    val message = Option(outboundMessages.poll())

    message.foreach(_.send(socket))

    message.nonEmpty
  }

  /**
   * Retrieves the next inbound message (if available) and invokes the
   * inbound message callback.
   *
   * @param socket The socket whose next incoming message to retrieve
   */
  protected def processNextInboundMessage(
    socket: ZMQ.Socket,
    flags: Int = ZMQ.DONTWAIT
  ): Unit = {
    Option(ZMsg.recvMsg(socket, flags)).foreach(zMsg => {
      inboundMessageCallback.foreach(_(zMsg.asScala.toSeq    
        .map(zFrame => zFrame.getData)  
      ))
    })
  }

  /**
   * Creates a new instance of a ZMQ Socket.
   *
   * @param zmqContext The context to use to create the socket
   * @param socketType The type of socket to create
   *
   * @return The new ZMQ.Socket instance
   */
  protected def newZmqSocket(zmqContext: ZMQ.Context, socketType: Int) =
    zmqContext.socket(socketType)

  override def run(): Unit = {
    val socket = newZmqSocket(context, socketType.`type`)//context.socket(socketType.`type`)

    try {
      processOptions(socket)

      while (notClosed) {
        Try(processNextOutboundMessage(socket)).failed.foreach(
          logger.error("Failed to send next outgoing message!", _: Throwable)
        )
        Try(processNextInboundMessage(socket)).failed.foreach({
          e: Throwable => {
            e match {
              case ex: java.lang.IllegalStateException => {
//                if (ex.getMessage != "Cannot receive another request") {
//                  logger.error("Failed to retrieve next incoming message!", e)
//                } else {
//                  /* Swallow this common exception */
//                }
              }
              case _ => logger.error("Failed to retrieve next incoming message!", e)
            }
        }})
        Thread.sleep(1)
      }
    } catch {
      case ex: Exception =>
        logger.error("Unexpected exception in 0mq socket runnable!", ex)
    } finally {
      try{
        socket.close()
      } catch {
        case ex: Exception =>
          logger.error("Failed to close socket!", _: Throwable)
      }
    }
  }

  /**
   * Marks the runnable as closed such that it eventually stops processing
   * messages and closes the socket.
   *
   * @throws AssertionError If the runnable is not processing messages or has
   *                        already been closed
   */
  override def close(): Unit = {
    assert(_isProcessing && notClosed,
      "Runnable is not processing or is closed!")

    _isProcessing = false
    notClosed = false
  }
}
