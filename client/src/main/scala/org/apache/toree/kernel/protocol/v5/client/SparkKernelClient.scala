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

package org.apache.toree.kernel.protocol.v5.client

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.apache.toree.comm._
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.kernel.protocol.v5.client.execution.{DeferredExecution, ExecuteRequestTuple}
import org.apache.toree.kernel.protocol.v5.client.socket.HeartbeatMessage
import org.apache.toree.kernel.protocol.v5.client.socket.StdinClient.{ResponseFunctionMessage, ResponseFunction}
import org.apache.toree.kernel.protocol.v5.content.ExecuteRequest
import org.apache.toree.utils.LogLike
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * Client API for Spark Kernel.
 *
 * Note: This takes a moment to initialize an actor system, so take appropriate action
 * if an API is to be used immediately after initialization.
 *
 * The actorSystem parameter allows shutdown of this client's ActorSystem.
 */
class SparkKernelClient(
  private val actorLoader: ActorLoader,
  private val actorSystem: ActorSystem,
  private val commRegistrar: CommRegistrar
) extends LogLike {
  implicit val timeout = Timeout(21474835.seconds)

  /**
   * Executes code on the Spark Kernel.
   * Gives a <code>DeferredExecution</code> used to handle results from
   * code execution. Specifically it can be used to register
   * callbacks that handle stream results, the code execution result, or code
   * execution errors.
   *
   * Code that prints to stdout is considered streaming and will be sent to all
   * callbacks registered with the onStream() method. For example:
   * <code>
   * client.execute("println(1)").onStream(someFunction)
   * </code>
   * someFunction will receive a
   * `org.apache.toree.kernel.protocol.v5.content.StreamContent` message:
   * <code>
   * {
   *  "name" : "stdout",
   *  "text" : "1"
   * }
   * </code>
   *
   * Code that produces a result will cause invocation of the callbacks
   * registered with the onResult() method. The callbacks will be invoked with
   * the result of the executed code. For example:
   * <code>
   * client.execute("1+1").onResult(someFunction)
   * </code>
   * someFunction will receive a
   * [[org.apache.toree.kernel.protocol.v5.content.ExecuteResult]] message:
   * <code>
   * {
   *  "execution_count" : 1,
   *  "data" : {
   *    "text/plain" : "2"
   *  },
   *  "metadata" : {}
   * }
   * </code>
   *
   * Code that produces an error will be sent to all callbacks registered
   * with the onResult() method. For example:
   * <code>
   * client.execute("1+1").onResult(someFunction)
   * </code>
   * someFunction will be invoked with an
   * [[org.apache.toree.kernel.protocol.v5.content.ExecuteReply]] message
   * containing the error.
   *
   * @param code Scala code
   * @return The DeferredExecution associated with the code execution.
   */
  def execute(code: String): DeferredExecution = {
    val request = ExecuteRequest(code, false, true, UserExpressions(), true)
    val de = new DeferredExecution
    actorLoader.load(MessageType.Incoming.ExecuteRequest) ! ExecuteRequestTuple(request, de)
    de
  }

  /**
   * Sets the response function used when input is requested by the kernel.
   * @param responseFunc The response function to use
   */
  def setResponseFunction(responseFunc: ResponseFunction): Unit = {
    actorLoader.load(SocketType.StdInClient) !
      ResponseFunctionMessage(responseFunc)
  }

  /**
   * Represents the exposed interface for Comm communication with the kernel.
   */
  val comm = new ClientCommManager(
    actorLoader = actorLoader,
    kmBuilder = KMBuilder(),
    commRegistrar = commRegistrar
  )

  // TODO: hide this? just heartbeat to see if kernel is reachable?
  def heartbeat(failure: () => Unit): Unit = {
    val future = actorLoader.load(SocketType.Heartbeat) ? HeartbeatMessage

    future.onComplete {
      case Success(_) =>
        logger.info("Client received heartbeat.")
      case Failure(_) =>
        failure()
        logger.info("There was an error receiving heartbeat from kernel.")
    }
  }

  def shutdown() = {
    logger.info("Shutting down client")
    actorSystem.terminate()
  }
}