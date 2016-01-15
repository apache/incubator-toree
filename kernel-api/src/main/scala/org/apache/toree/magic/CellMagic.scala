package org.apache.toree.magic

/**
 * Cell Magics change the output of a cell in IPython
 */
trait CellMagic extends Magic {
  override def execute(code: String): CellMagicOutput
}
