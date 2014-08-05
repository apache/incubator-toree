package com.ibm.spark.kernel.protocol.v5

package object security {
  object SignatureManagerChildActorType extends Enumeration {
    type SignatureManagerChildActorType = Value

    val SignatureChecker  = Value("signature_checker")
    val SignatureProducer = Value("signature_producer")
  }
}
