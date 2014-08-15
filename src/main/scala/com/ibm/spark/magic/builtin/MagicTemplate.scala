package com.ibm.spark.magic.builtin


/**
 * Represents the base structure for a magic that is loaded and executed.
 */
trait MagicTemplate {
  /**
   * Execute a magic representing a line magic.
   * @param code The single line of code
   * @return The output of the magic
   */
  def executeLine(code: String): String

  /**
   * Execute a magic representing a cell magic.
   * @param code The list of code, separated by newlines
   * @return The output of the magic
   */
  def executeCell(code: Seq[String]): String
}
