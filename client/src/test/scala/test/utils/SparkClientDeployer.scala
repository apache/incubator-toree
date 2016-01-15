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

package test.utils

import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.apache.toree.comm.{CommRegistrar, CommStorage}
import org.apache.toree.kernel.protocol.v5.client.socket._
import org.apache.toree.kernel.protocol.v5.client.{ActorLoader, SparkKernelClient}
import org.apache.toree.kernel.protocol.v5.client.boot.ClientBootstrap
import org.apache.toree.kernel.protocol.v5.client.boot.layers.{StandardHandlerInitialization, StandardSystemInitialization}
import org.apache.toree.kernel.protocol.v5.SocketType
import org.apache.toree.utils.LogLike
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Represents an object that can deploy a singleton Spark Client for tests,
 * providing access to the actors used for socket communication.
 */
object SparkClientDeployer extends LogLike{

  private val profileJson: String = """
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

  private var testActorSystem: ActorSystem = _
  private var testActorLoader: ActorLoader = _
  private var heartbeatProbe: TestProbe = _
  private var stdinProbe: TestProbe = _
  private var shellProbe: TestProbe = _
  private var ioPubProbe: TestProbe = _

  private class ActorInterceptor(testProbe: TestProbe, actor: ActorRef)
    extends Actor
  {
    override def receive: Receive = {
      case m =>
        testProbe.ref.forward(m)
        actor ! m
    }
  }

  /**
   * Runs system initialization, wrapping socket actors with test logic to
   * intercept messages.
   */
  private trait ExposedSystemInitialization
    extends StandardSystemInitialization with LogLike
  {
    override def initializeSystem(
      config: Config, actorSystem: ActorSystem, actorLoader: ActorLoader,
      socketFactory: SocketFactory):
    (ActorRef, ActorRef, ActorRef, ActorRef, CommRegistrar, CommStorage) = {
      val signatureEnabled = config.getString("key").nonEmpty
      val commStorage = new CommStorage()
      val commRegistrar = new CommRegistrar(commStorage)

      heartbeatProbe = new TestProbe(actorSystem)
      val heartbeatClient = actorSystem.actorOf(
        Props(classOf[HeartbeatClient], socketFactory, actorLoader, signatureEnabled)
      )
      val heartbeatInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(heartbeatProbe, heartbeatClient)),
        name = SocketType.HeartbeatClient.toString
      )

      stdinProbe = new TestProbe(actorSystem)
      val stdinClient = actorSystem.actorOf(
        Props(classOf[StdinClient], socketFactory, actorLoader, signatureEnabled)
      )
      val stdinInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(stdinProbe, stdinClient)),
        name = SocketType.StdInClient.toString
      )

      shellProbe = new TestProbe(actorSystem)
      val shellClient = actorSystem.actorOf(
        Props(classOf[ShellClient], socketFactory, actorLoader, signatureEnabled)
      )
      val shellInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(shellProbe, shellClient)),
        name = SocketType.ShellClient.toString
      )

      ioPubProbe = new TestProbe(actorSystem)
      val ioPubClient = actorSystem.actorOf(
        Props(classOf[IOPubClient], socketFactory, actorLoader, signatureEnabled,
          commRegistrar, commStorage)
      )
      val ioPubInterceptor = actorSystem.actorOf(
        Props(new ActorInterceptor(ioPubProbe, ioPubClient)),
        name = SocketType.IOPubClient.toString
      )

      testActorSystem = actorSystem
      testActorLoader = actorLoader

      (heartbeatInterceptor, stdinInterceptor, shellInterceptor,
        ioPubInterceptor, commRegistrar, commStorage)
    }
  }

  /**
   * Represents the internal singleton ClientBootstrap instance that does not
   * receive any external commandline arguments.
   */
  private lazy val client = {
    // Print out a message to indicate this fixture is being created
    logger.debug("Creating 'no external args' Spark Client through Client Bootstrap")

    val clientBootstrap =
      new ClientBootstrap(ConfigFactory.parseString(profileJson))
        with ExposedSystemInitialization
        with StandardHandlerInitialization

    logger.debug("Finished initializing Client Bootstrap! Testing can now start!")

    clientBootstrap.createClient()
  }

  /**
   * Provides a gateway for tests to receive the ClientBootstrap through a
   * client and socket test probes.
   *
   * @param testCode The test code to execute
   *
   * @return The results from the test code
   */
  def withSparkClient(
    testCode: (SparkKernelClient, ActorLoader,
      TestProbe, TestProbe, TestProbe, TestProbe) => Any
  ) = testCode(client, testActorLoader,
    heartbeatProbe, stdinProbe, shellProbe, ioPubProbe)


  /**
   * Retrieves the actor system for the ClientBootstrap instance. Will
   * initialize the ClientBootstrap if the actor system is not ready.
   *
   * @return The actor system of ClientBootstrap
   */
  def getClientActorSystem: ActorSystem =
    if (testActorSystem == null) { client; testActorSystem }
    else testActorSystem
}
