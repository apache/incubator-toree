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

package org.apache.toree.kernel.api

/**
 * Represents the methods available to display data of different mime-types from the kernel to the
 * client.
 */
trait DisplayMethodsLike {

  /**
   * Send a display message of the specified mime-type and content
   * @param mimeType The mime-type of the content (i.e. text/html)
   * @param data The content to send for display
   */
  def content(mimeType: String, data: String): Unit

  /**
   * Send html content to the client
   * @param data The content to send for display
   */
  def html(data: String): Unit = content("text/html", data)

  /**
   * Send javascript content to the client
   * @param data The content to send for display
   */
  def javascript(data: String): Unit = content("application/javascript", data)

  /**
   * Sends a clear-output message to client
   * @param wait if true, client waits for next display_data to clear
   */
  def clear(wait: Boolean = false): Unit
}
