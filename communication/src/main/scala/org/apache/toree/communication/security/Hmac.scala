/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.communication.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.toree.communication.security.HmacAlgorithm.HmacAlgorithm

object HmacAlgorithm extends Enumeration {
  type HmacAlgorithm = Value

  def apply(key: String) = Value(key)

  val MD5     = Value("HmacMD5")
  val SHA1    = Value("HmacSHA1")
  val SHA256  = Value("HmacSHA256")
}

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
