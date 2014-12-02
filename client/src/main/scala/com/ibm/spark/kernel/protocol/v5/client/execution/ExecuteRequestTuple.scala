package com.ibm.spark.kernel.protocol.v5.client.execution

import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest

case class ExecuteRequestTuple(request: ExecuteRequest, de: DeferredExecution)
