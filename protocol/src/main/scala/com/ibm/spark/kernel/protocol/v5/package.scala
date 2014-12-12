/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol

import com.ibm.spark.kernel.protocol.v5.MIMEType.MIMEType

package object v5 {
  // Provide a UUID type representing a string (there is no object)
  type UUID = String

  // Provide a ParentHeader type and object representing a Header
  type ParentHeader = Header
  val ParentHeader = Header

  // Provide a Metadata type and object representing a map
  type Metadata = Map[String, String]
  val Metadata = Map

  // Provide a Data type and object representing a map
  type Data = Map[MIMEType, String]
  val Data = Map

  // Provide a UserExpressions type and object representing a map
  type UserExpressions = Map[String, String]
  val UserExpressions = Map

  // Provide a Payloads type and object representing a list of maps
  type Payloads = List[Map[String, String]]
  val Payloads = List


  // TODO: Split this into client/server socket types and move them to their
  //       respective projects
  object SocketType extends Enumeration {
    type SocketType = Value

    // Server-specific actors
    val Shell       = Value("shell")
    val IOPub       = Value("io_pub")
    val StdIn       = Value("std_in")
    val Control     = Value("control")
    val Heartbeat   = Value("heartbeat")

    // Client-specific actors
    val ShellClient       = Value("shell_client")
    val IOPubClient       = Value("io_pub_client")
    val StdInClient       = Value("std_in_client")
    val ControlClient     = Value("control_client")
    val HeartbeatClient   = Value("heartbeat_client")
  }

  object MessageType extends Enumeration {
    type MessageType    = Value

    //  Shell Router/Dealer Messages
    val CompleteRequest = Value("complete_request")
    val CompleteReply   = Value("complete_reply")
    val ConnectRequest  = Value("connect_request")
    val ConnectReply    = Value("connect_reply")
    val ExecuteRequest  = Value("execute_request")
    val ExecuteReply    = Value("execute_reply")
    val HistoryRequest  = Value("history_request")
    val HistoryReply    = Value("history_reply")
    val InspectRequest  = Value("inspect_request")
    val InspectReply    = Value("inspect_reply")
    val KernelInfoRequest  = Value("kernel_info_request")
    val KernelInfoReply    = Value("kernel_info_reply")
    val ShutdownRequest = Value("shutdown_request")
    val ShutdownReply   = Value("shutdown_reply")

    //  Pub/Sub Messages
    val ClearOutput     = Value("clear_output")
    val DisplayData     = Value("display_data")
    val Error           = Value("error")
    val ExecuteInput    = Value("execute_input")
    val ExecuteResult   = Value("execute_result")
    val Status          = Value("status")
    val Stream          = Value("stream")
    val CommOpen        = Value("comm_open")
    val CommMsg         = Value("comm_msg")
    val CommClose       = Value("comm_close")
  }

  object HandlerType extends Enumeration {
    type HandlerType = Value

    val ExecuteRequestHandler = Value("execute_request_handler")
  }

  object SystemActorType extends Enumeration {
    type SystemActorType = Value

    val KernelMessageRelay  = Value("kernel_message_relay")
    val ExecuteRequestRelay = Value("execute_request_relay")
    val Interpreter         = Value("interpreter")
    val SignatureManager    = Value("signature_manager")
    val MagicManager        = Value("magic_manager")
    val StatusDispatch      = Value("status_dispatch")
  }

  object KernelStatusType extends Enumeration {
    type KernelStatusType = Value

    val Starting  = Value("starting")
    val Busy      = Value("busy")
    val Idle      = Value("idle")
  }

  object MIMEType extends Enumeration {
    type MIMEType = String

    val PlainText               = """text/plain"""
    val ImagePng                = """image/png"""
    val TextHtml                = """text/html"""
    val ApplicationJson         = """application/json"""
    val ApplicationJavaScript   = """application/javascript"""
  }
}