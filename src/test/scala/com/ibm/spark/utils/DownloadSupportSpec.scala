package com.ibm.spark.utils

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
    File(testFileName).deleteIfExists()
    File(downloadDestinationUrl.getPath).deleteIfExists()
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
        File(badFilename).deleteIfExists()
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
