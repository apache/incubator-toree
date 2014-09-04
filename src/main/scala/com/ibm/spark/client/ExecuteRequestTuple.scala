package com.ibm.spark.client

import com.ibm.spark.kernel.protocol.v5.content.ExecuteRequest

case class ExecuteRequestTuple(request: ExecuteRequest, callback: Any => Unit)
