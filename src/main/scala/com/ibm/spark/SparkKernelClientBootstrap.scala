package com.ibm.spark

import java.io.File

import akka.actor.{ActorRef, Props, ActorSystem}
import com.ibm.spark.client.SparkKernelClient
import com.ibm.spark.client.handler.ExecuteHandler
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5.{MessageType, SocketType, SimpleActorLoader}
import com.ibm.spark.kernel.protocol.v5.socket._
import com.ibm.spark.utils.LogLike

/**
 * Created by Chris on 8/12/14.
 */
object SparkKernelClientBootstrap extends LogLike {

  // set up our actor system and configure the socket factory
  private val actorSystem = ActorSystem("spark-client-actor-system")
  private val actorLoader = SimpleActorLoader(actorSystem)

  private val profile = Option(new File("src/main/resources/profile.json"))
  private val socketConfigReader = new SocketConfigReader(profile)

  private val socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)

  private var heartbeatClientActor: Option[ActorRef] = None

  /**
   * @return an instance of a SparkKernelClient
   */
  def createClient: SparkKernelClient = {
    new SparkKernelClient(actorLoader)
  }

  /**
   * Initializes all kernel systems.
   */
  def initialize(): Unit = {
    initializeSystemActors
    initializeMessageHandlers
  }

  /**
   * Shuts down all kernel systems.
   */
  def shutdown = {
    logger.info("Shutting down actor system")
    actorSystem.shutdown()

    this
  }

  private def initializeSystemActors(): Unit = {
    heartbeatClientActor = Option(actorSystem.actorOf(Props(classOf[HeartbeatClient], socketFactory),
      name = SocketType.HeartbeatClient.toString))

    actorSystem.actorOf(Props(classOf[ShellClient], socketFactory),
      name = SocketType.ShellClient.toString)

    actorSystem.actorOf(Props(classOf[IOPubClient], socketFactory),
      name = SocketType.IOPubClient.toString)
  }

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType) {
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(Props(clazz, actorLoader), name = messageType.toString)
  }

  private def initializeMessageHandlers(): Unit = {
    initializeRequestHandler(classOf[ExecuteHandler], MessageType.ExecuteRequest)
  }

  initialize()
}
