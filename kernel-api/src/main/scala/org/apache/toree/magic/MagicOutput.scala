package org.apache.toree.magic

/**
 * Represents the output of a magic execution.
 */
case class MagicOutput(data: (String, String)*) {
  lazy val asMap: Map[String, String] = Map(data:_*)
}
