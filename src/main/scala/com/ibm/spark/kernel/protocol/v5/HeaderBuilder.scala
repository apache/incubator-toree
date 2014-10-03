package com.ibm.spark.kernel.protocol.v5

object HeaderBuilder {
  /**
   * Creates a new Header instance with the provided id and type.
   * @param msgType The type of the message
   * @param msgId (Optional) The unique identifier of the message, generates a
   *              random UUID if none is provided
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

  /**
   * Represents an "empty" header where the message type and id are blank.
   */
  val empty = create("", "")
}
