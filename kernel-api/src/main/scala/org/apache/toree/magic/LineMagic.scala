package org.apache.toree.magic

/**
 * Line Magics perform some function and don't return anything. I.e. you cannot
 * do  `val x = %runMyCode 1 2 3` or alter the MIMEType of the cell.
 */
trait LineMagic extends Magic {
  override def execute(code: String): Unit
}
