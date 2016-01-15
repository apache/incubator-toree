package org.apache.toree.magic.dependencies

import org.apache.toree.magic.Magic
import org.apache.spark.sql.SQLContext

trait IncludeSQLContext {
  this: Magic =>

  private var _sqlContext: SQLContext = _
  def sqlContext: SQLContext = _sqlContext
  def sqlContext_=(newSqlContext: SQLContext) =
    _sqlContext = newSqlContext
}
