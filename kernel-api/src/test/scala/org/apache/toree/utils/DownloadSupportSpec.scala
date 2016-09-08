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

import java.io.FileNotFoundException
import java.net.URL

import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import scala.io.Source
import scala.tools.nsc.io.File

class DownloadSupportSpec extends FunSpec with Matchers with BeforeAndAfter {
  val downloadDestinationUrl = new URL("file:///tmp/testfile2.ext")

  val testFileContent = "This is a test"
  val testFileName = "/tmp/testfile.txt"

  //  Create a test file for downloading
  before {
    File(testFileName).writeAll(testFileContent)
  }

  //  Cleanup what we made
  after {
    if (File(testFileName).exists) File(testFileName).delete()
    if (File(downloadDestinationUrl.getPath).exists) File(downloadDestinationUrl.getPath).delete()
  }

  describe("DownloadSupport"){
    describe("#downloadFile( String, String )"){
      it("should download a file to the download directory"){
        val testFileUrl = "file:///tmp/testfile.txt"

        //  Create our utility and download the file
        val downloader = new Object with DownloadSupport
        downloader.downloadFile(
          testFileUrl,
          downloadDestinationUrl.getProtocol + "://" +
            downloadDestinationUrl.getPath)

        //  Verify the file contents are what was in the original file
        val downloadedFileContent: String =
          Source.fromFile(downloadDestinationUrl.getPath).mkString

        downloadedFileContent should be (testFileContent)
      }

    }

    describe("#downloadFile( URL, URL )"){
      it("should download a file to the download directory"){
        val testFileUrl = new URL("file:///tmp/testfile.txt")

        val downloader = new Object with DownloadSupport
        downloader.downloadFile(testFileUrl, downloadDestinationUrl)

        //  Verify the file contents are what was in the original file
        val downloadedFileContent: String =
          Source.fromFile(downloadDestinationUrl.getPath).mkString

        downloadedFileContent should be (testFileContent)
      }

      it("should throw FileNotFoundException if the download URL is bad"){
        val badFilename = "file:///tmp/testbadfile.txt"
        if (File(badFilename).exists) File(badFilename).delete()

        val badFileUrl = new URL(badFilename)

        val downloader = new Object with DownloadSupport
        intercept[FileNotFoundException] {
          downloader.downloadFile(badFileUrl, downloadDestinationUrl)
        }
      }

      it("should throw FileNotFoundException if the download ") {
        val testFileUrl = new URL("file:///tmp/testfile.txt")
        val badDestinationUrl =
          new URL("file:///tmp/badloc/that/doesnt/exist.txt")

        val downloader = new Object with DownloadSupport
        intercept[FileNotFoundException] {
          downloader.downloadFile(testFileUrl, badDestinationUrl)
        }
      }
    }
  }

}
