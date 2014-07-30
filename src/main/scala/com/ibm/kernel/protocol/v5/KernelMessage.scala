package com.ibm.kernel.protocol.v5

case class KernelMessage(
  ids : List[String],
  header : Header,
  parentHeader: ParentHeader,
  metadata: Metadata,
  contentString : String
  ) {}
