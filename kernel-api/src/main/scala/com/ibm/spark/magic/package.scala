package com.ibm.spark

package object magic {
  /**
   * Represents the output of a magic execution.
   */
  // TODO: This is a duplicate of Data in kernel protocol, needs to be given
  //       a type/val that can be translated into a specific protocol via
  //       implicits - or some other transformation - to separate this from
  //       the protocol type
  type MagicOutput = Map[String, String]
  val MagicOutput = Map
}
