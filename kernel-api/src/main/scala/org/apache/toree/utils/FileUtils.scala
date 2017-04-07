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

package org.apache.toree.utils

import java.io.File
import java.nio.file.Files


object FileUtils {

  val parentTempDir: String = "toree-tmp-dir"

  private def deleteDirRecur(file: File): Unit = {
    // delete directory recursively
    if(file.isDirectory){
      file.listFiles.foreach(deleteDirRecur)
    }
    if(file.exists){
      file.delete
    }
  }

  private def createParentTemp() = {
    val dir = Files.createTempDirectory(parentTempDir).toFile
    sys.addShutdownHook{
      // addShutdownHook will ensure when JVM exits, the temporary directory
      // assoicated with a given kernel would be deleted
      deleteDirRecur(dir)
    }
    dir
  }

  private lazy val parent: File = createParentTemp()

  /**
    * Create a directory with name specified under the "toree-tmp-dir"
    * that created by Files.createTempDirectory
    *
    * @param name The name of the directory to be created
    */
  def createManagedTempDirectory(name: String): File = {
    val dir = new File(parent, name)
    dir.mkdir()
    dir
  }
}
