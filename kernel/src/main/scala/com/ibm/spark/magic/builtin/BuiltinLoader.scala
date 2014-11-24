package com.ibm.spark.magic.builtin

import com.google.common.reflect.ClassPath
import com.ibm.spark.magic.InternalClassLoader
import com.google.common.base.Strings._
import scala.collection.JavaConversions._

/**
 * Represents a class loader that loads classes from the builtin package.
 */
class BuiltinLoader
  extends InternalClassLoader(classOf[BuiltinLoader].getClassLoader) {

  private val pkgName = this.getClass.getPackage.getName
  
  def getClasses(pkg: String = pkgName) = {
    isNullOrEmpty(pkg) match {
      case true =>
        List()
      case false =>
        val classPath = ClassPath.from(this.getClass.getClassLoader)
        classPath.getTopLevelClasses(pkg).filter(
          _.getSimpleName != this.getClass.getSimpleName
        )
    }
  }
}
