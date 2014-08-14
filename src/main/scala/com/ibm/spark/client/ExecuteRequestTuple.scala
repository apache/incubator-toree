package com.ibm.spark.client

import com.ibm.spark.client.exception.ShellException
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteResult, ExecuteRequest}

/**
 * Created by Chris on 8/11/14.
 */
case class ExecuteRequestTuple(request: ExecuteRequest,
                               resultCallback: ExecuteResult => Any,
                               errorCallback: ShellException => Unit)
