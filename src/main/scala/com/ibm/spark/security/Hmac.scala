package com.ibm.spark.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacAlgorithm extends Enumeration {
  type HmacAlgorithm = Value

  def apply(key: String) = Value(key)

  val MD5     = Value("HmacMD5")
  val SHA1    = Value("HmacSHA1")
  val SHA256  = Value("HmacSHA256")
}

import HmacAlgorithm._

object Hmac {
  
  def apply(key: String, algorithm: HmacAlgorithm = HmacAlgorithm.SHA256) =
    new Hmac(key, algorithm)
  
  def newMD5(key: String): Hmac     = this(key, HmacAlgorithm.MD5)
  def newSHA1(key: String): Hmac    = this(key, HmacAlgorithm.SHA1)
  def newSHA256(key: String): Hmac  = this(key, HmacAlgorithm.SHA256)
}

class Hmac(
  val key: String,
  val algorithm: HmacAlgorithm = HmacAlgorithm.SHA256
) {

  private var mac: Mac = _
  private var secretKeySpec: SecretKeySpec = _

  if (key.nonEmpty) {
    mac = Mac.getInstance(algorithm.toString)
    secretKeySpec = new SecretKeySpec(key.getBytes, algorithm.toString)
    mac.init(secretKeySpec)
  }

  def apply(items: String*): String = digest(items)

  def digest(items: Seq[String]): String = if (key.nonEmpty) {
    mac synchronized {
      items.map(_.getBytes("UTF-8")).foreach(mac.update)
      mac.doFinal().map("%02x" format _).mkString
    }
  } else ""
}
