package com.ibm.spark.utils

import java.net.URL
import java.nio.channels._
import java.io.FileOutputStream

/**
 * A utility for downloading the contents of a file to a specified location.
 */
class FileDownloadUtil {

  /**
   * Download a file located at the given URL to the specified destination file.
   * The file type of the downloadDestination should match the file type
   * of the file located at fileUrl. Throws a FileNotFoundException if the
   * fileUrl or downloadDestination are invalid.
   *
   * @param fileUrl A URL for the file to be downloaded
   * @param downloadDestination Location to download the file to (e.g. /tmp/file.txt)
   */
  def download(fileUrl: URL, downloadDestination: String): Unit = {
    val rbc = Channels.newChannel(fileUrl.openStream())
    val fos = new FileOutputStream(downloadDestination)
    fos.getChannel().transferFrom(rbc, 0, Long.MaxValue)
  }

  /**
   * Download a file given a URL string to the specified downloadDestination.
   * @param fileToDownload A URL in string format (e.g. file:///tmp/foo, http://ibm.com)
   * @param downloadDestination Location to download the file to (e.g. /tmp/file.txt)
   */
  def download(fileToDownload: String, downloadDestination: String): Unit = {
    download(new URL(fileToDownload), downloadDestination)
  }

}
