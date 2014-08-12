package com.ibm.spark.magic

import java.net.{URL, URLClassLoader}

class MagicLoader(urls: Array[URL] = Array(), parentLoader: ClassLoader = null)
  extends URLClassLoader(urls, parentLoader)
{
  private val MagicExecuteMethodName = "execute"

  def hasMagic(name: String): Boolean =
    try {
      this.findClass(name)
      true
    } catch {
      case _: Throwable => false
    }

  def executeMagic(name: String, code: String, isCell: Boolean): String = {
    val klass: Class[_] = findClass(name)
    val method = klass.getDeclaredMethod(
      MagicExecuteMethodName, classOf[String], classOf[Boolean]
    )

    val results = method.invoke(
      klass.newInstance(),
      Seq(code, isCell).map(_.asInstanceOf[AnyRef]): _*
    )
    results.asInstanceOf[String]
  }

}
