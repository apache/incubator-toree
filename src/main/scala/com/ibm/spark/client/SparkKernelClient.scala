package com.ibm.spark.client

import java.io.File

import akka.actor.{Props, ActorSystem}
import com.ibm.spark.kernel.protocol.v5.MessageType._
import com.ibm.spark.kernel.protocol.v5.{SocketType, SimpleActorLoader}
import com.ibm.spark.kernel.protocol.v5.SocketType._
import com.ibm.spark.kernel.protocol.v5.handler.GenericSocketMessageHandler
import com.ibm.spark.kernel.protocol.v5.socket.{HeartbeatClient, SocketFactory, SocketConfigReader}
import com.ibm.spark.utils.LogLike

/**
 * Created by dalogsdon on 8/5/14.
 */
class SparkKernelClient extends LogLike {
  val actorSystem = ActorSystem("spark-client-actor-system")
  val actorLoader = SimpleActorLoader(actorSystem)

  val profile = Option(new File("src/main/resources/profile.json"))
  val socketConfigReader = new SocketConfigReader(profile)

  val socketFactory = new SocketFactory(socketConfigReader.getSocketConfig)

  // test with a heartbeat client
  val heartbeatClientActor = actorSystem.actorOf(Props(classOf[HeartbeatClient], socketFactory),
    name = SocketType.HeartbeatClient.toString)

  private def initializeRequestHandler[T](clazz: Class[T], messageType: MessageType) {
    logger.info("Creating %s handler".format(messageType.toString))
    actorSystem.actorOf(
      Props(clazz, actorLoader),
      name = messageType.toString
    )
  }

  private def initializeSocketHandler(socketType: SocketType, messageType: MessageType): Unit = {
    logger.info("Creating %s to %s socket handler ".format(messageType.toString, socketType.toString))
    actorSystem.actorOf(
      Props(classOf[GenericSocketMessageHandler], actorLoader, socketType),
      name = messageType.toString
    )
  }

  //  private def initializeKernelHandlers(): Unit = {
  //    //  These are the handlers for messages coming into the
  //    initializeRequestHandler(classOf[ExecuteRequestHandler], MessageType.ExecuteRequest )
  //    initializeRequestHandler(classOf[KernelInfoRequestHandler], MessageType.KernelInfoRequest )
  //
  //    //  These are handlers for messages leaving the kernel through the sockets
  //    initializeSocketHandler(SocketType.Shell, MessageType.KernelInfoReply)
  //    initializeSocketHandler(SocketType.Shell, MessageType.ExecuteReply)
  //    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteResult)
  //    initializeSocketHandler(SocketType.IOPub, MessageType.ExecuteInput)
  //    initializeSocketHandler(SocketType.IOPub, MessageType.Status)
  //  }
}