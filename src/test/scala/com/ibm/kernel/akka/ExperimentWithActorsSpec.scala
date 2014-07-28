package com.ibm.kernel.akka

import scala.concurrent.duration._
import akka.actor._
import org.scalatest.{FunSpec, Matchers}

/**
 * User: ginobustelo
 */
class ExperimentWithActorsSpec extends FunSpec with Matchers{

  case class Message(msgType:String, msgBody:String)

  class RawRequest extends Actor with ActorLogging {
    def receive = {
      case "" => {
        log.info( "Oops, empty message!")
      }
      case raw: String => {
        //parse the message and send to router
        log.info( s"Got raw message $raw")

        val split: Array[String] = raw.split(" ")

        val msg: Message = Message(split(0), raw )
        context.system.actorSelection("/user/router") ! msg
      }
      case _ => {
        log.info( "Oops!")
      }
    }
  }

  class MessageRouter extends Actor with ActorLogging {
    def receive = {
      case msg: Message => {
        log.info( s"Got message $msg")

        msg.msgType match {
          case "stop" => {
            context.system.actorSelection("/user/handler_stop") ! msg
          }
          case _ => {
            context.system.actorSelection("/user/handler_default") ! msg
          }
        }
      }
      case _ => {
        log.info( "Oops!")
      }
    }
  }

  class MessageHandler extends Actor with ActorLogging {
    def receive = {
      case msg: Message => {
        log.info( s"Handle message type ${msg.msgType} and body ${msg.msgBody}")
      }
      case _ => {
        log.info( "Oops!")
      }
    }
  }

  class StopMessageHandler extends Actor with ActorLogging {
    def receive = {
      case _ => {
        log.info( "Shutting down...")
        context.system.shutdown()
      }
    }
  }


  describe("#experiment") {
    it("sample akka set of actors handling messages") {
      val system = ActorSystem("experiment")

      val raw: ActorRef = system.actorOf(Props(new RawRequest), "raw")
      system.actorOf(Props(new MessageRouter), "router")
      system.actorOf(Props(new MessageHandler), "handler_default")
      system.actorOf(Props(new MessageHandler), "handler_stop")


      new Thread( new Runnable{
        def run(){
          1 to 10 foreach {
            i => raw ! "sometype message"+i
            Thread.sleep(1.second.toMillis);
          }

          raw ! "stop"
        }
      }).start

      try {
        system.awaitTermination(1.minute)
      }
      catch {
        case _ => system.shutdown()
      }

      println("DONE!")

    }
  }
}
