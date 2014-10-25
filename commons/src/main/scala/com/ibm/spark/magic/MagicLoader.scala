package com.ibm.spark.magic

import java.net.{URL, URLClassLoader}

import com.ibm.spark.magic.dependencies.DependencyMap

import scala.reflect.runtime.{universe => runtimeUniverse}

class MagicLoader(
  val dependencyMap: DependencyMap = new DependencyMap(),
  urls: Array[URL] = Array(),
  parentLoader: ClassLoader = null
) extends URLClassLoader(urls, parentLoader)
{
  def hasMagic(name: String): Boolean =
    try {
      this.loadClass(name) // Checks parent loadClass first
      true
    } catch {
      case _: Throwable => false
    }

  protected def createMagicInstance(name: String) = {
    val magicClass = loadClass(name) // Checks parent loadClass first

    val runtimeMirror = runtimeUniverse.runtimeMirror(magicClass.getClassLoader)

    val classSymbol = runtimeMirror.staticClass(magicClass.getCanonicalName)
    val classMirror = runtimeMirror.reflectClass(classSymbol)
    val selfType = classSymbol.selfType

    val classConstructorSymbol =
      selfType.declaration(runtimeUniverse.nme.CONSTRUCTOR).asMethod
    val classConstructorMethod =
      classMirror.reflectConstructor(classConstructorSymbol)

    val magicInstance = classConstructorMethod()

    // Add all of our dependencies to the new instance
    dependencyMap.internalMap.filter(selfType <:< _._1).values.foreach(
      _(magicInstance.asInstanceOf[MagicTemplate])
    )

    magicInstance
  }

  def executeMagic(name: String, code: String, isCell: Boolean): MagicOutput = {
    val magicInstance = createMagicInstance(name).asInstanceOf[MagicTemplate]

    if (isCell) {
      magicInstance.executeCell(code.split("\n"))
    } else {
      magicInstance.executeLine(code)
    }
  }
}
