package test.utils

import akka.actor.Actor
import akka.testkit.TestProbe

class TestProbeProxyActor(probe: TestProbe) extends Actor {
  override def receive = {
    case m =>
      probe.ref ! m
  }
}