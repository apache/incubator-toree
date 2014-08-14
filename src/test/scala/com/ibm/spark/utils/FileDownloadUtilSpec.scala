package com.ibm.spark.utils

import java.io.FileNotFoundException
import java.net.URL

import org.scalatest.{BeforeAndAfter, Matchers, FunSpec}
import scala.io.Source
import scala.tools.nsc.io.File

class FileDownloadUtilSpec extends FunSpec with Matchers with BeforeAndAfter {
  val downloadDestination: String = "/tmp/testfile2.ext"
  val testFileContent = "This is a test"
  val testFileName = "/tmp/testfile.txt"

  //  Create a test file for downloading
  before {
    File(testFileName).writeAll(testFileContent)
  }

  //  Cleanup what we made
  after {
    File(testFileName).deleteIfExists()
    File(downloadDestination).deleteIfExists()
  }

  describe("FileDownloadUtil"){
    describe("#download( String, String )"){
      it("should download a file to the download directory"){
        val testFileUrl = "file:///tmp/testfile.txt"

        //  Create our utility and download the file
        val fdu = new FileDownloadUtil
        fdu.download(testFileUrl, downloadDestination)

        //  Verify the file contents are what was in the original file
        val downloadedFileContent: String = Source.fromFile(downloadDestination).mkString

        downloadedFileContent should be (testFileContent)
      }
    }

    describe("#download( URL, String )"){
      it("should download a file to the download directory"){
        val testFileUrl = new URL("file:///tmp/testfile.txt")

        //  Create our utility and download the file
        val fdu = new FileDownloadUtil
        fdu.download(testFileUrl, downloadDestination)

        //  Verify the file contents are what was in the original file
        val downloadedFileContent: String = Source.fromFile(downloadDestination).mkString

        downloadedFileContent should be (testFileContent)
      }
    }

    describe("#download( BadFileString, String )"){
      it("should throw FileNotFoundException"){
        val badFilename = "file:///tmp/testbadfile.txt"
        File(badFilename).deleteIfExists()
        val badFileUrl = new URL(badFilename)
        //  Create our utility and download the file
        val fdu = new FileDownloadUtil

        // verify that a FileNotFoundException is thrown
        try {
          fdu.download(badFileUrl, downloadDestination)
          fail("Did not throw an exception.")
        } catch {
          case fnfe: FileNotFoundException =>
          case _: Throwable => fail("Another exception was thrown.")
        }
      }
    }

    describe("#download( Url, BadDownloadLocation)"){
      it("should throw FileNotFoundException") {
        val testFileUrl = new URL("file:///tmp/testfile.txt")
        //  Create our utility and download the file
        val fdu = new FileDownloadUtil
        val badDownloadDestination: String = "/tmp/badloc/that/doesnt/exist.txt"

        // verify that a FileNotFoundException is thrown
        try {
          fdu.download(testFileUrl, badDownloadDestination)
          fail("Did not throw an exception.")
        } catch {
          case fnfe: FileNotFoundException =>
          case _: Throwable => fail("Another exception was thrown.")
        }
      }
    }
  }

}
