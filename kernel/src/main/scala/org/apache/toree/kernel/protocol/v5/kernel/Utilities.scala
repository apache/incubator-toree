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

package org.apache.toree.kernel.protocol.v5.kernel

import java.nio.charset.Charset

import akka.util.{ByteString, Timeout}
import org.apache.toree.communication.ZMQMessage
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.utils.LogLike
import play.api.libs.json.JsonValidationError
import play.api.libs.json.{JsPath, Json, Reads}

import scala.concurrent.duration._

object Utilities extends LogLike {
  //
  // NOTE: This is brought in to remove feature warnings regarding the use of
  //       implicit conversions regarding the following:
  //
  //       1. ByteStringToString
  //       2. ZMQMessageToKernelMessage
  //
  import scala.language.implicitConversions

  /**
   * This timeout needs to be defined for the Akka asks to timeout
   */
  implicit val timeout = Timeout(21474835.seconds)

  implicit def ByteStringToString(byteString : ByteString) : String = {
    new String(byteString.toArray, Charset.forName("UTF-8"))
  }

  implicit def StringToByteString(string : String) : ByteString = {
    ByteString(string.getBytes)
  }

  implicit def ZMQMessageToKernelMessage(message: ZMQMessage): KernelMessage = {
    val delimiterIndex: Int =
      message.frames.indexOf(ByteString("<IDS|MSG>".getBytes))
    //  TODO Handle the case where there is no delimiter
    val ids: Seq[Array[Byte]] =
      message.frames.take(delimiterIndex).map(
        (byteString : ByteString) =>  { byteString.toArray }
      )
    val header = Json.parse(message.frames(delimiterIndex + 2)).as[Header]
    // TODO: Investigate better solution than setting parentHeader to null for {}
    val parentHeader = parseAndHandle(message.frames(delimiterIndex + 3),
                                  ParentHeader.headerReads,
                                  handler = (valid: ParentHeader) => valid,
                                  errHandler = _ => null
    )
    val metadata = Json.parse(message.frames(delimiterIndex + 4)).as[Metadata]

    KMBuilder().withIds(ids.toList)
               .withSignature(message.frame(delimiterIndex + 1))
               .withHeader(header)
               .withParentHeader(parentHeader)
               .withMetadata(metadata)
               .withContentString(message.frame(delimiterIndex + 5)).build(false)
  }

  implicit def KernelMessageToZMQMessage(kernelMessage : KernelMessage) : ZMQMessage = {
    val frames: scala.collection.mutable.ListBuffer[ByteString] = scala.collection.mutable.ListBuffer()
    kernelMessage.ids.map((id : Array[Byte]) => frames += ByteString.apply(id) )
    frames += "<IDS|MSG>"
    frames += kernelMessage.signature
    frames += Json.toJson(kernelMessage.header).toString()
    frames += Json.toJson(kernelMessage.parentHeader).toString()
    frames += Json.toJson(kernelMessage.metadata).toString
    frames += kernelMessage.contentString
    ZMQMessage(frames  : _*)
  }

  def parseAndHandle[T, U](json: String, reads: Reads[T],
                           handler: T => U) : U = {
    parseAndHandle(json, reads, handler,
      (invalid: Seq[(JsPath, Seq[JsonValidationError])]) => {
        logger.error(s"Could not parse JSON, ${json}")
        throw new Throwable(s"Could not parse JSON, ${json}")
      }
    )
  }

  def parseAndHandle[T, U](json: String, reads: Reads[T],
                           handler: T => U,
                           errHandler: Seq[(JsPath, Seq[JsonValidationError])] => U) : U = {
    Json.parse(json).validate[T](reads).fold(
      errHandler,
      (content: T) => handler(content)
    )
  }
}
