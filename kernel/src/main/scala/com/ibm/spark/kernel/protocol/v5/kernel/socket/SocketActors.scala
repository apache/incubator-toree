package com.ibm.spark.kernel.protocol.v5.kernel.socket

import java.nio.charset.Charset

import akka.actor.{ActorRef, Actor}
import akka.util.ByteString
import com.ibm.spark.kernel.protocol.v5.kernel.ZMQMessage
import org.zeromq.{ZFrame, ZMsg, ZMQ}

class PubSocketActor(connection: String) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val publisher = zmqContext.socket(ZMQ.PUB)

  publisher.bind(connection)
  publisher.setLinger(0)

  override def postStop(): Unit = {
    publisher.close()
    zmqContext.term()
  }

  override def receive: Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString => new String(byteString.toArray, Charset.forName("UTF-8")))
      ZMsg.newStringMsg(frames: _*).send(publisher)
//      if (zmqMessage.frames.length > 1) {
//        zmqMessage.frames.take(zmqMessage.frames.length - 1).foreach(byteString =>
//          publisher.sendMore(byteString.toArray))
//      }
//
//      publisher.send(zmqMessage.frames.last.toArray)
  }
}


class RouterSocketActor(connection: String, listener: ActorRef) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val router = zmqContext.socket(ZMQ.ROUTER)

  router.bind(connection)
  router.setLinger(0)

  override def postStop(): Unit = {
    router.close()
    zmqContext.term()
  }

  new Thread(new Runnable {
    override def run(): Unit = while (!Thread.interrupted()) {
      val recvMsg: ZMsg = ZMsg.recvMsg(router)
      if (recvMsg != null) {
        import scala.collection.JavaConverters._
        val frames = recvMsg.asScala.toSeq.map(_.getData).map(ByteString.apply)
        listener ! ZMQMessage(frames: _*)
      }
    }
  }).start()

  override def receive: Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString =>
        new String(byteString.toArray, Charset.forName("UTF-8")))
      ZMsg.newStringMsg(frames: _*).send(router)
  }
}


class ReplySocketActor(connection: String, listener: ActorRef) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val router = zmqContext.socket(ZMQ.REP)

  router.bind(connection)
  router.setLinger(0)

  override def postStop(): Unit = {
    router.close()
    zmqContext.term()
  }

  new Thread(new Runnable {
    override def run(): Unit = while (!Thread.interrupted()) {
      val recvMsg: ZMsg = ZMsg.recvMsg(router)
      if (recvMsg != null) {
        import scala.collection.JavaConverters._
        val frames = recvMsg.asScala.toSeq.map(_.getData).map(ByteString.apply)
        listener ! ZMQMessage(frames: _*)
      }
    }
  }).start()

  override def receive: Receive = {
    case zmqMessage: ZMQMessage =>
      val frames = zmqMessage.frames.map(byteString =>
        new String(byteString.toArray, Charset.forName("UTF-8")))
      ZMsg.newStringMsg(frames: _*).send(router)
  }
}