package com.ibm.spark.kernel.protocol.v5.client.socket

import java.nio.charset.Charset

import akka.actor.{ActorRef, Actor}
import akka.util.ByteString
import com.ibm.spark.kernel.protocol.v5.client.ZMQMessage
import org.zeromq.{ZMsg, ZMQ}

class SubSocketActor(connection: String, listener: ActorRef) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val subscriber = zmqContext.socket(ZMQ.SUB)

  subscriber.connect(connection)
  subscriber.setLinger(0)
  subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL)

  override def postStop(): Unit = {
    subscriber.close()
    zmqContext.term()
  }

  new Thread(new Runnable {
    override def run(): Unit = while (!Thread.interrupted()) {
      val recvMsg: ZMsg = ZMsg.recvMsg(subscriber)
      if (recvMsg != null) {
        import scala.collection.JavaConverters._
        val frames = recvMsg.asScala.toSeq.map(_.getData).map(ByteString.apply)
        listener ! ZMQMessage(frames: _*)
      }
    }
  }).start()

  override def receive: Receive = {
    case _ =>
  }
}


class ReqSocketActor(connection: String, listener: ActorRef) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val requester = zmqContext.socket(ZMQ.REQ)

  requester.connect(connection)
  requester.setLinger(0)

  override def postStop(): Unit = {
    requester.close()
    zmqContext.term()
  }

  new Thread(new Runnable {
    override def run(): Unit = while (!Thread.interrupted()) {
      val recvMsg: ZMsg = ZMsg.recvMsg(requester)
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
      ZMsg.newStringMsg(frames: _*).send(requester)
  }
}

class DealerSocketActor(identity: String, connection: String, listener: ActorRef) extends Actor {
  //  TODO: Can this be pushed into a guardian style pattern
  val zmqContext = ZMQ.context(1)
  val dealer = zmqContext.socket(ZMQ.DEALER)

  dealer.connect(connection)
  dealer.setLinger(0)
  dealer.setIdentity(identity.getBytes(ZMQ.CHARSET))

  override def postStop(): Unit = {
    dealer.close()
    zmqContext.term()
  }

  new Thread(new Runnable {
    override def run(): Unit = while (!Thread.interrupted()) {
      val recvMsg: ZMsg = ZMsg.recvMsg(dealer)
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
      ZMsg.newStringMsg(frames: _*).send(dealer)
  }
}