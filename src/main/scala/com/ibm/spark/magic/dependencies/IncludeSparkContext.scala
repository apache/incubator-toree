package com.ibm.spark.magic.dependencies

import com.ibm.spark.magic.builtin.MagicTemplate
import org.apache.spark.SparkContext

trait IncludeSparkContext {
  this: MagicTemplate =>

  //val sparkContext: SparkContext
  private var _sparkContext: SparkContext = _
  def sparkContext: SparkContext = _sparkContext
  def sparkContext_=(newSparkContext: SparkContext) = _sparkContext = newSparkContext
}
