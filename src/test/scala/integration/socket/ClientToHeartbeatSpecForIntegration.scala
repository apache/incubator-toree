package integration.socket

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.{ByteString, Timeout}
import akka.zeromq._
import com.ibm.spark.kernel.protocol.v5.socket.{Heartbeat, SocketConfig, SocketFactory}
import org.scalatest.{FunSpec, Matchers}
import test.utils.{BlockingStack, StackActor}

import scala.concurrent.duration._

class ClientToHeartbeatSpecForIntegration extends FunSpec with Matchers{
  implicit val timeout = Timeout(5.seconds)

  val system = ActorSystem("HeartbeatClient")
  val socketConfig = SocketConfig(-1,-1, 8000, -1, -1, "127.0.0.1", "tcp","hmac-sha256","")
  val socketFactory : SocketFactory = SocketFactory(socketConfig)
  val heartbeat = system.actorOf(Props(classOf[Heartbeat], socketFactory), "Heartbeat")
  val stack =  new BlockingStack()
  val clientSocket : ActorRef = socketFactory.HeartbeatClient(system,
    system.actorOf(Props(classOf[StackActor], stack), "Queue"))

  describe("Client-Heartbeat Integration"){
    describe("Client"){
      it("should connect to Heartbeat Socket"){
          stack.pop() should be (Connecting)
    	}

      it("should send message and receive echoed message"){
        clientSocket ! ZMQMessage(ByteString("<STRING>".getBytes()))
        val zMQMessage :  ZMQMessage = stack.pop().asInstanceOf[ZMQMessage]
        val messageText = new String(zMQMessage.frame(0).toArray)
         messageText should be ("<STRING>")
      }
    }
  }
}
