package com.ibm.spark.kernel.protocol.v5.socket

import java.io.File

import play.api.libs.json._

import scala.io.Source

class SocketConfigReader(profile: Option[File]) {
  val profileFile =
    Source.fromFile(
      profile.getOrElse(new File("src/main/resources/profile.json")))
  val profileJson = Json.parse(profileFile.mkString)
  val socketConfig = profileJson.as[SocketConfig]

  def getSocketConfig: SocketConfig = {
    socketConfig
  }
}
