package com.ibm.spark.kernel.protocol.v5.client.message

import com.ibm.spark.kernel.protocol.v5.UUID

case class StreamMessage (
  id: UUID,
  callback: Any => Unit
)