package com.ibm.spark.client.message

import com.ibm.spark.kernel.protocol.v5.UUID

case class StreamMessage (
  id: UUID,
  callback: Any => Unit
)