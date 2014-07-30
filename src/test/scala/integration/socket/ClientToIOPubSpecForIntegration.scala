package integration.socket

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.util.Timeout
import akka.zeromq.Connecting
import com.ibm.kernel.protocol.v5.socket.{IOPub, Heartbeat, SocketFactory, SocketConfig}
import org.scalatest.{FunSpec, Matchers}
import test.utils.{StackActor, BlockingStack}

import scala.concurrent.duration._

class ClientToIOPubSpecForIntegration extends FunSpec with Matchers {
  implicit val timeout = Timeout(5 seconds)

  val system = ActorSystem("IOPubClient")
  val socketConfig = SocketConfig(-1,-1, -1, -1, 8002, "127.0.0.1", "tcp","hmac-sha256","")
  val socketFactory : SocketFactory = SocketFactory(socketConfig)
  val iopub = system.actorOf(Props(classOf[IOPub], socketFactory), "IOPub")
  val stack =  new BlockingStack()
  val clientSocket : ActorRef = socketFactory.HeartbeatClient(system,
    system.actorOf(Props(classOf[StackActor], stack), "IOPubQueue"))

  describe("Client-IOPub Integration"){
    describe("Client"){
      it("should connect to IOPub Socket"){
          stack.pop() should be (Connecting)
    	}

      // todo: some meaningful test
    }
  }
}
