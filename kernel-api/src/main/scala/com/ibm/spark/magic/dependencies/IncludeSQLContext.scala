package com.ibm.spark.magic.dependencies

import com.ibm.spark.magic.Magic
import org.apache.spark.sql.SQLContext

trait IncludeSQLContext {
  this: Magic =>

  private var _sqlContext: SQLContext = _
  def sqlContext: SQLContext = _sqlContext
  def sqlContext_=(newSqlContext: SQLContext) =
    _sqlContext = newSqlContext
}
