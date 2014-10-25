package com.ibm.spark.magic.dependencies

import com.ibm.spark.dependencies.DependencyDownloader
import com.ibm.spark.magic.MagicTemplate

trait IncludeDependencyDownloader {
  this: MagicTemplate =>

  private var _dependencyDownloader: DependencyDownloader = _
  def dependencyDownloader: DependencyDownloader = _dependencyDownloader
  def dependencyDownloader_=(newDependencyDownloader: DependencyDownloader) =
    _dependencyDownloader = newDependencyDownloader
}
