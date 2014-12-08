package com.ibm.spark.magic

/**
 * Represents a classloader that can load classes from within.
 *
 * @param classLoader The classloader to use for internal retrieval
 *                    (defaults to self's classloader)
 */
class InternalClassLoader(
  classLoader: ClassLoader = classOf[InternalClassLoader].getClassLoader
) extends ClassLoader(classLoader) {

  // TODO: Provides an exposed reference to the super loadClass to be stubbed
  // out in tests.
  private[magic] def parentLoadClass(name: String, resolve: Boolean) =
    super.loadClass(name, resolve)

  /**
   * Attempts to load the class using the local package of the builtin loader
   * as the base of the name if unable to load normally.
   *
   * @param name The name of the class to load
   * @param resolve If true, then resolve the class
   *
   * @return The class instance of a ClassNotFoundException
   */
  override def loadClass(name: String, resolve: Boolean): Class[_] =
    try {
      val packageName = this.getClass.getPackage.getName
      val className = name.split('.').last

      parentLoadClass(packageName + "." + className, resolve)
    } catch {
      case ex: ClassNotFoundException =>
        parentLoadClass(name, resolve)
    }
}
