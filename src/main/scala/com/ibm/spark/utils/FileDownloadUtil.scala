package com.ibm.spark.utils

import java.net.URL
import java.nio.channels._
import java.io.FileOutputStream

class FileDownloadUtil {
  def download(fileUrl: URL, downloadDestination: String): Unit = {
    val rbc = Channels.newChannel(fileUrl.openStream())
    val fos = new FileOutputStream(downloadDestination)
    fos.getChannel().transferFrom(rbc, 0, Long.MaxValue)
  }

  def download(fileToDownload: String, downloadDestination: String): Unit = {
    download(new URL(fileToDownload), downloadDestination)
  }

}
