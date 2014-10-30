package com.ibm.spark.magic.builtin

import com.ibm.spark.magic.InternalClassLoader

/**
 * Represents a class loader that loads classes from the builtin package.
 */
class BuiltinLoader
  extends InternalClassLoader(classOf[BuiltinLoader].getClassLoader)
