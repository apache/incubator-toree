package com.ibm.spark.kernel.protocol.v5

package object interpreter {
  object InterpreterChildActorType extends Enumeration {
    type InterpreterChildActorType = Value

    val ExecuteRequestTask = Value("execute_request_task")
    val CodeCompleteTask = Value("code_complete_task")
  }
}
