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

package org.apache.toree.utils

import joptsimple.util.KeyValuePair

/**
 * Provides utility methods for jsimple-opt key pair values.
 */
object KeyValuePairUtils {
  val DefaultDelimiter = ","

  /**
   * Transforms the provided string into a list of KeyValuePair objects.
   * @param keyValuePairString The string representing the series of keys and
   *                           values
   * @param delimiter The delimiter used for splitting up the string
   * @return The list of KeyValuePair objects
   */
  def stringToKeyValuePairSeq(
    keyValuePairString: String,
    delimiter: String = DefaultDelimiter
  ): Seq[KeyValuePair] = {
    require(keyValuePairString != null, "KeyValuePair string cannot be null!")

    keyValuePairString
      .split(delimiter)
      .map(_.trim)
      .filterNot(_.isEmpty)
      .map(KeyValuePair.valueOf)
      .toSeq
  }

  /**
   * Transforms the provided list of KeyValuePair elements into a string.
   * @param keyValuePairSeq The sequence of KeyValuePair objects
   * @param delimiter The separator between string KeyValuePair
   * @return The resulting string from the list of KeyValuePair objects
   */
  def keyValuePairSeqToString(
    keyValuePairSeq: Seq[KeyValuePair],
    delimiter: String = DefaultDelimiter
  ): String = {
    require(keyValuePairSeq != null, "KeyValuePair sequence cannot be null!")

    keyValuePairSeq
      .map(pair => pair.key.trim + "=" + pair.value.trim)
      .mkString(delimiter)
  }
}
