/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.spark.toree.test.utils

import sys.process._
import java.net.URL
import java.io.File
import java.util.UUID

import scala.language.postfixOps

import org.apache.spark.TestUtils.{JavaSourceFromString, _}

object JarUtils {

  def createTemporaryDir() = {
    val tempDir = System.getProperty("java.io.tmpdir")
    val dir = new File(tempDir, UUID.randomUUID.toString)
    dir.mkdirs()
    dir.deleteOnExit()
    dir.getCanonicalFile
  }

  def createDummyJar(destDir: String, packageName: String, className: String) : URL = {
    val srcDir = new File(destDir, packageName)
    srcDir.mkdirs()
    val source =
        s"""package $packageName;
            |
            |public class $className implements java.io.Serializable {
            |  public static String sayHello(String arg) { return "Hello, " + arg; }
            |  public static int addStuff(int arg1, int arg2) { return arg1 + arg2; }
            |}
         """.stripMargin

    val sourceFile =
      new JavaSourceFromString(new File(srcDir, className).toURI.getPath, source)
    val compiledFile = createCompiledClass(className, srcDir, sourceFile, Seq.empty)
    val jarFile = new File(destDir,
        s"$packageName-$className-%s.jar".format(System.currentTimeMillis()))
    val jarURL = createJar(Seq(compiledFile), jarFile, directoryPrefix = Some(packageName))
    jarFile.deleteOnExit()
    jarURL
  }

  def downloadJar(destDir: String, artifactURL: String) : URL = {
    val fileName = getFileName(artifactURL).
      replace(".jar", s"%s.jar".format(System.currentTimeMillis()))
    val jarFile = new File(destDir, fileName)
    jarFile.deleteOnExit()
    (new URL(artifactURL) #> jarFile !!)
    jarFile.toURI.toURL
  }

  private def getFileName(artifactURL: String) = {
    artifactURL.split("/").last
  }
}
