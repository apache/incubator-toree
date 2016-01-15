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

import java.net.URL
import java.nio.channels._
import java.io.FileOutputStream

/**
 * A utility for downloading the contents of a file to a specified location.
 */
trait DownloadSupport {
  /**
   * Download a file located at the given URL to the specified destination file.
   * The file type of the downloadDestination should match the file type
   * of the file located at fileUrl. Throws a FileNotFoundException if the
   * fileUrl or downloadDestination are invalid.
   *
   * @param fileUrl A URL for the file to be downloaded
   * @param destinationUrl Location to download the file to (e.g. /tmp/file.txt)
   *
   * @return The URL representing the location of the downloaded file
   */
  def downloadFile(fileUrl: URL, destinationUrl: URL): URL = {
    val rbc = Channels.newChannel(fileUrl.openStream())
    val fos = new FileOutputStream(destinationUrl.getPath)
    fos.getChannel.transferFrom(rbc, 0, Long.MaxValue)

    destinationUrl
  }

  /**
   * Download a file given a URL string to the specified downloadDestination.
   *
   * @param fileToDownload A URL in string format (e.g. file:///tmp/foo, http://ibm.com)
   * @param destination Location to download the file to (e.g. /tmp/file.txt)
   *
   * @return The URL representing the location of the downloaded file
   */
  def downloadFile(fileToDownload: String, destination: String): URL = {
    downloadFile(new URL(fileToDownload), new URL(destination))
  }
}
