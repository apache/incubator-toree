package com.ibm.spark.kernel.protocol.v5

case class KernelMessage(
  ids: Seq[String],
  signature: String,
  header: Header,
  parentHeader: ParentHeader, // TODO: This can be an empty json object of {}
  metadata: Metadata,
  contentString: String
)
