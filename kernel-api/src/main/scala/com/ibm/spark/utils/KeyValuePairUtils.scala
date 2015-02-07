/*
 * Copyright 2015 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.utils

import joptsimple.util.KeyValuePair

/**
 * Provides utility methods for jsimple-opt key pair values.
 */
object KeyValuePairUtils {
  val DefaultDelimiter = ","

  /**
   * Transforms the provided string into a list of KeyValuePair objects.
   * @param keyPairString The string representing the series of keys and values
   * @param delimiter The delimiter used for splitting up the string
   * @return The list of KeyValuePair objects
   */
  def stringToKeyValuePairSeq(
    keyPairString: String,
    delimiter: String = DefaultDelimiter
  ): Seq[KeyValuePair] =
    keyPairString
      .split(delimiter)
      .map(_.trim)
      .filterNot(_.isEmpty)
      .map(KeyValuePair.valueOf)
      .toSeq

  /**
   * Transforms the provided list of KeyValuePair elements into a string.
   * @param keyValuePairList The list of KeyValuePair
   * @param delimiter The separator between string KeyValuePair
   * @return The resulting string from the list of KeyValuePair objects
   */
  def keyValuePairSeqToString(
    keyValuePairList: Seq[KeyValuePair],
    delimiter: String = DefaultDelimiter
  ): String =
    keyValuePairList
      .map(pair => pair.key + "=" + pair.value)
      .mkString(delimiter)
}
