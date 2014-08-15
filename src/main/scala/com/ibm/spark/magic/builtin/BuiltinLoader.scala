package com.ibm.spark.magic.builtin

class BuiltinLoader
  extends ClassLoader(classOf[BuiltinLoader].getClassLoader)
{
  /**
   * Attempts to load the class using the local package of the builtin loader
   * as the base of the name if unable to load normally.
   * @param name The name of the class to load
   * @param resolve If true, then resolve the class
   * @return The class instance of a ClassNotFoundException
   */
  override def loadClass(name: String, resolve: Boolean): Class[_] =
    try {
      val packageName = this.getClass.getPackage.getName
      val classNameRegex = """.*?\.?(\w+)$""".r
      val classNameRegex(className) = name
      super.loadClass(packageName + "." + className, resolve)
    } catch {
      case ex: ClassNotFoundException =>
        super.loadClass(name, resolve)
    }
}
