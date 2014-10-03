package com.ibm.spark.kernel.protocol.v5

object HeaderBuilder {

  /**
   * Creates a new Header instance with the provided id and type.
   * @param msgType The type of the message
   * @param msgId (Optional) The unique identifier of the message
   * @return The new Header instance
   */
  def create(
    msgType: String,
    msgId: UUID = java.util.UUID.randomUUID.toString
  ) = Header(
      msgId,
      SparkKernelInfo.username,
      SparkKernelInfo.session,
      msgType,
      SparkKernelInfo.protocolVersion
    )

}
