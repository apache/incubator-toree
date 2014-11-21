package com.ibm.spark.magic.builtin

import com.google.common.reflect.ClassPath
import com.ibm.spark.magic.InternalClassLoader

import scala.collection.JavaConversions._



/**
 * Represents a class loader that loads classes from the builtin package.
 */
class BuiltinLoader
  extends InternalClassLoader(classOf[BuiltinLoader].getClassLoader) {

  def getBuiltinClasses = {
    val classPath = ClassPath.from(this.getClass.getClassLoader)
    classPath.getTopLevelClasses("com.ibm.spark.magic.builtin").filter(
      _.getSimpleName != this.getClass.getSimpleName
    )
  }
}
