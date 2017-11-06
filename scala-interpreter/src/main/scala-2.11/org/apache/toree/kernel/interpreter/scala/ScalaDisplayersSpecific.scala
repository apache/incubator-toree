package org.apache.toree.kernel.interpreter.scala

import scala.reflect.runtime.universe

/**
  * Provides Scala version-specific features needed for the [[ScalaDisplayers]].
  */
private[scala] object ScalaDisplayersSpecific {
  /**
    * Returns a term name for the specified name.
    *
    * @param name the name
    *
    * @return the term name
    */
  private[scala] def getTermName(name: String) = {
    universe.TermName(name)
  }
}
