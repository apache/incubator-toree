package integration.socket

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.zeromq._
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest
import com.ibm.spark.kernel.protocol.v5.socket._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

class ClientToShellSpecForIntegration extends TestKit(ActorSystem("ShellActorSpec"))
with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  describe("ShellActor") {
    val socketFactory = mock[SocketFactory]
    val actorLoader = mock[ActorLoader]
    val probe : TestProbe = TestProbe()
    val probeClient : TestProbe = TestProbe()
    when(socketFactory.Shell(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probe.ref)
    when(socketFactory.ShellClient(
      any(classOf[ActorSystem]), any(classOf[ActorRef])
    )).thenReturn(probeClient.ref)
    when(actorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(probe.ref.path.toString))

    val shell = system.actorOf(Props(
      classOf[Shell], socketFactory, actorLoader
    ))
    val shellClient = system.actorOf(Props(
      classOf[ShellClient], socketFactory
    ))

    val request = ExecuteRequest(
      """val x = "foo"""", false, true, UserExpressions(), true
    )
    val header = Header(
      UUID.randomUUID().toString, "spark",
      UUID.randomUUID().toString, MessageType.ExecuteRequest.toString,
      "5.0"
    )
    val kernelMessage = KernelMessage(
      Seq[String](), "", header, HeaderBuilder.empty, Metadata(),
      Json.toJson(request).toString
    )

    describe("send execute request") {
      it("should send execute request") {
        shellClient ! kernelMessage
        probeClient.expectMsgClass(classOf[ZMQMessage])
        probeClient.forward(shell)
        probe.expectMsgClass(classOf[Tuple2[Seq[_], KernelMessage]])
        probe.forward(shellClient)
      }
    }
  }
}
