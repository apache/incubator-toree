package com.ibm.spark.communication

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, BeforeAndAfter, Matchers, FunSpecLike}

object PubSocketActorSpec {
  val config = """
    |akka {
    |loglevel = "WARNING"
    |}
  """.stripMargin.trim
}

class PubSocketActorSpec extends TestKit(
  ActorSystem(
    "PubSocketActorSpec",
    ConfigFactory.parseString(PubSocketActorSpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter
  with MockitoSugar with OneInstancePerTest
{
//  private val

  describe("PubSocketActor") {

  }
}
