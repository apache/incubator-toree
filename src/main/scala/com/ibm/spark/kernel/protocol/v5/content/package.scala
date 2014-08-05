package com.ibm.spark.kernel.protocol.v5

package object content {
  // Provide an ExecuteReplyOk type and object representing a
  // partially-completed ExecuteReply
  //
  // TODO: Is there a way to wrap the Option arguments in Some(...)?
  //       E.g. ExecuteReplyOk(3, [], {}) =>
  //            ExecuteReply("ok", 3, Some([]), Some({}), None, None, None
  type ExecuteReplyOk = ExecuteReply
  val ExecuteReplyOk = ExecuteReply(
    "ok", _: Int, _: Option[Payloads],
    _: Option[UserExpressions], None, None, None
  )

  // Provide an ExecuteReplyError type and object representing a
  // partially-completed ExecuteReply
  type ExecuteReplyError = ExecuteReply
  val ExecuteReplyError = ExecuteReply(
    "error", _: Int, None, None, _: Option[String],
    _: Option[String], _: Option[List[String]]
  )

  // Provide an ExecuteReplyAbort type and object representing a
  // partially-completed ExecuteReply
  type ExecuteReplyAbort = ExecuteReply
  val ExecuteReplyAbort = ExecuteReply(
    "abort", _: Int, None, None, None, None, None
  )

  // Provide an InspectReplyOk type and object representing a
  // partially-completed InspectReply
  type InspectReplyOk = InspectReply
  val InspectReplyOk = InspectReply(
    "ok", _: Data, _: Metadata, None, None, None
  )

  // Provide an InspectReplyOk type and object representing a
  // partially-completed InspectReply
  type InspectReplyError = InspectReply
  val InspectReplyError = InspectReply(
    "error", _: Data, _: Metadata, _: Option[String],
    _: Option[String], _: Option[List[String]]
  )

  // Provide an CompleteReplyOk type and object representing a
  // partially-completed CompleteReply
  type CompleteReplyOk = CompleteReply
  val CompleteReplyOk = CompleteReply(
    _: List[String], _: Int, _: Int, _: Metadata, "ok", None, None, None
  )

  // Provide an CompleteReplyError type and object representing a
  // partially-completed CompleteReply
  type CompleteReplyError = CompleteReply
  val CompleteReplyError = CompleteReply(
    _: List[String], _: Int, _: Int, _: Metadata, "error", _: Option[String],
    _: Option[String], _: Option[List[String]]
  )

  // ShutdownReply message is exactly the same format as ShutdownRequest
  type ShutdownReply = ShutdownRequest
  val ShutdownReply = ShutdownRequest

}
