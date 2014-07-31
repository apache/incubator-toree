package integration.socket

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.util.Timeout
import akka.zeromq._
import com.ibm.kernel.protocol.v5.socket.{Heartbeat, SocketFactory, SocketConfig, Shell}
import org.scalatest.{FunSpec, Matchers}
import test.utils.{StackActor, BlockingStack}

import scala.concurrent.duration._

class ClientToShellSpecForIntegration extends FunSpec with Matchers {
  implicit val timeout = Timeout(5.seconds)

  val system = ActorSystem("ShellClient")
  val socketConfig = SocketConfig(-1,-1, -1, 8000, -1, "127.0.0.1", "tcp","hmac-sha256","")
  val socketFactory : SocketFactory = SocketFactory(socketConfig)
  val heartbeat = system.actorOf(Props(classOf[Heartbeat], socketFactory), "Shell")
  val stack =  new BlockingStack()
  val clientSocket : ActorRef = socketFactory.HeartbeatClient(system,
    system.actorOf(Props(classOf[StackActor], stack), "Queue"))

  describe("Client-Shell Integration"){
    describe("Client"){
      it("should connect to Shell Socket"){
          stack.pop() should be (Connecting)
    	}

      // todo: some meaningful test
    }
  }
}
