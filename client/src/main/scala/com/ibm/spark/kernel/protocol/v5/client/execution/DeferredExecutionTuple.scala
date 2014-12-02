package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.UUID

case class DeferredExecutionTuple ( id: UUID, de: DeferredExecution)