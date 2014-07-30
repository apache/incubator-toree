package com.ibm.kernel.protocol.v5

case class KernelMessage(
  ids : Seq[String],
  signature : String,
  header : Header,
  parentHeader: ParentHeader,
  metadata: Metadata,
  contentString : String
  ) {}
