/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.toree.dependencies

import java.io.PrintStream
import java.net.URL

abstract class DependencyDownloader(repositoryUrl: String, baseDir: String) {

  /**
   * Retrieves the dependency and all of its dependencies as jars.
   *
   * @param groupId The group id associated with the main dependency
   * @param artifactId The id of the dependency artifact
   * @param version The version of the main dependency
   * @param transitive If true, downloads all dependencies of the specified
   *                   dependency
   * @param excludeBaseDependencies If true, will exclude any dependencies
   *                                included in the build of the kernel
   *
   * @return The sequence of strings pointing to the retrieved dependency jars
   */
  def retrieve(
    groupId: String, artifactId: String, version: String,
    transitive: Boolean = true, excludeBaseDependencies: Boolean = true
  ): Seq[URL]

  /**
   * Sets the printstream to log to.
   *
   * @param printStream The new print stream to use for output logging
   */
  def setPrintStream(printStream: PrintStream): Unit

}
