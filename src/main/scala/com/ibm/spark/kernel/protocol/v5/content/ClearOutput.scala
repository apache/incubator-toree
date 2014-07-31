package com.ibm.spark.kernel.protocol.v5.content

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ClearOutput (
  //  We cannot use _wait as a field because it is a method defined on all objects
  _wait: Boolean
)

//  Single property fields are not well supported by play, this is a little funky workaround founde here:
//  https://groups.google.com/forum/?fromgroups=#!starred/play-framework/hGrveOkbJ6U
object ClearOutput{
  implicit val clearOutputReads: Reads[ClearOutput] = (
    (JsPath \ "wait").read[Boolean].map(ClearOutput(_)))
  implicit val clearOutputWrites: Writes[ClearOutput] = (
    (JsPath \ "wait").write[Boolean].contramap((c : ClearOutput) => c._wait)
    )

}