package org.apache.toree.magic

import org.apache.toree.utils.DynamicReflectionSupport

import scala.language.dynamics

class MagicExecutor(magicLoader: MagicLoader) extends Dynamic {

  val executeMethod = classOf[Magic].getDeclaredMethods.head.getName

  def applyDynamic(name: String)(args: Any*): Either[CellMagicOutput, LineMagicOutput] = {
    val className = magicLoader.magicClassName(name)
    val isCellMagic = magicLoader.hasCellMagic(className)
    val isLineMagic = magicLoader.hasLineMagic(className)

    (isCellMagic, isLineMagic) match {
      case (true, false) =>
        val result = executeMagic(className, args)
        Left(result.asInstanceOf[CellMagicOutput])
      case (false, true) =>
        executeMagic(className, args)
        Right(LineMagicOutput)
      case (_, _) =>
        Left(CellMagicOutput("text/plain" ->
          s"Magic ${className} could not be executed."))
    }
  }

  private def executeMagic(className: String, args: Seq[Any]) = {
    val inst = magicLoader.createMagicInstance(className)
    val dynamicSupport = new DynamicReflectionSupport(inst.getClass, inst)
    dynamicSupport.applyDynamic(executeMethod)(args)
  }
}
