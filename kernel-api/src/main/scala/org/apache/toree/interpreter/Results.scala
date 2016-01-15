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

package org.apache.toree.interpreter

/**
 * Represents interpreter results, mostly taken from the
 * tools.nsc.interpreter.Results object.
 */
object Results {
  abstract sealed class Result

  /** The line was interpreted successfully. */
  case object Success extends Result { override def toString = "success" }

  /** The line was erroneous in some way. */
  case object Error extends Result { override def toString = "error" }

  /** The input was incomplete.  The caller should request more input. */
  case object Incomplete extends Result { override def toString = "incomplete" }

  /** The line was aborted before completed. */
  case object Aborted extends Result { override def toString = "aborted" }
}

