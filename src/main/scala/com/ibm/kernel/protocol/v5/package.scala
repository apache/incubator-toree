package com.ibm.kernel.protocol

package object v5 {
  // Provide a ParentHeader type and object representing a Header
  type ParentHeader = Header
  val ParentHeader = Header

  // Provide a Metadata type and object representing a map
  type Metadata = Map[String, String]
  val Metadata = Map

  // Provide a UserExpressions type and object representing a map
  type UserExpressions = Map[String, String]
  val UserExpressions = Map
}
