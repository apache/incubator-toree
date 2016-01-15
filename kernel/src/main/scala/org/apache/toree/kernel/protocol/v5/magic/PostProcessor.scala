package org.apache.toree.kernel.protocol.v5.magic

import org.apache.toree.interpreter.{ExecuteOutput, Interpreter}
import org.apache.toree.kernel.protocol.v5.{Data, MIMEType}
import org.apache.toree.magic.{CellMagicOutput, LineMagicOutput}
import org.apache.toree.utils.LogLike

class PostProcessor(interpreter: Interpreter) extends LogLike {
  val defaultErr = "Something went wrong in postprocessor!"

  def process(codeOutput: ExecuteOutput): Data = {
    interpreter.lastExecutionVariableName.flatMap(interpreter.read) match {
      case Some(l: Left[_, _]) => matchCellMagic(codeOutput, l)
      case Some(r: Right[_, _]) => matchLineMagic(codeOutput, r)
      case _ => Data(MIMEType.PlainText -> codeOutput)
    }
  }

  protected[magic] def matchCellMagic(code: String, l: Left[_,_]) =
    l.left.getOrElse(None) match {
      case cmo: CellMagicOutput => cmo
      case _ => Data(MIMEType.PlainText -> code)
    }

  protected[magic] def matchLineMagic(code: String, r: Right[_,_]) =
    r.right.getOrElse(None) match {
      case lmo: LineMagicOutput => processLineMagic(code)
      case _ => Data(MIMEType.PlainText -> code)
    }

  protected[magic] def processLineMagic(code: String): Data = {
    val parts = code.split("\n")
    Data(MIMEType.PlainText -> parts.take(parts.size - 1).mkString("\n"))
  }
}
