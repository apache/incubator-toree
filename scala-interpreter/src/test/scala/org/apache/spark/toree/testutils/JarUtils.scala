package org.apache.spark.toree.testutils

import java.io.File
import java.util.UUID

import org.apache.spark.TestUtils.{JavaSourceFromString, _}

object JarUtils {

  def createTemporaryDir() = {
    val tempDir = System.getProperty("java.io.tmpdir")
    val dir = new File(tempDir, UUID.randomUUID.toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir.getCanonicalFile
  }

  def createDummyJar(destDir: String, packageName: String, className: String) = {
    val srcDir = new File(destDir, packageName)
    srcDir.mkdirs()
    val excSource = new JavaSourceFromString(
      new File(srcDir, className).getAbsolutePath,
      s"""package $packageName;
        |
        |public class $className implements java.io.Serializable {
        |  public static String sayHello(String arg) { return "Hello, " + arg; }
        |  public static int addStuff(int arg1, int arg2) { return arg1 + arg2; }
        |}
     """.
            stripMargin)
    val excFile = createCompiledClass(className, srcDir, excSource, Seq.empty)
    val jarFile = new File(destDir,
      s"$packageName-$className-%s.jar".format(System.currentTimeMillis()))
    val jarURL = createJar(Seq(excFile), jarFile, directoryPrefix = Some(packageName))
    jarFile.deleteOnExit()
    jarURL
  }



}
