package com.ibm.spark.kernel.protocol.v5.client

import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest

case class ExecuteRequestTuple(request: ExecuteRequest, callback: Any => Unit)
