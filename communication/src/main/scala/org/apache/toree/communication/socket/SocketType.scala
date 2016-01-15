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
package org.apache.toree.communication.socket

import org.zeromq.ZMQ

/**
 * Represents the type option used to indicate the type of socket to create.
 *
 * @param `type` The type as an integer
 */
sealed class SocketType(val `type`: Int)

/** Represents a publish socket. */
case object PubSocket extends SocketType(ZMQ.PUB)

/** Represents a subscribe socket. */
case object SubSocket extends SocketType(ZMQ.SUB)

/** Represents a reply socket. */
case object RepSocket extends SocketType(ZMQ.REP)

/** Represents a request socket. */
case object ReqSocket extends SocketType(ZMQ.REQ)

/** Represents a router socket. */
case object RouterSocket extends SocketType(ZMQ.ROUTER)

/** Represents a dealer socket. */
case object DealerSocket extends SocketType(ZMQ.DEALER)
