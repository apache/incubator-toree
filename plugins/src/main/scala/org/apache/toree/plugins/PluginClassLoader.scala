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
package org.apache.toree.plugins

import java.net.{URLClassLoader, URL}

/**
 * Represents a class loader used to manage classes used as plugins.
 *
 * @param urls The initial collection of URLs pointing to paths to load
 *             plugin classes
 * @param parentLoader The parent loader to use as a fallback to load plugin
 *                     classes
 */
class PluginClassLoader(
  private val urls: Seq[URL],
  private val parentLoader: ClassLoader
) extends URLClassLoader(urls.toArray, parentLoader) {
  /**
   * Adds a new URL to be included when loading plugins. If the url is already
   * in the class loader, it is ignored.
   *
   * @param url The url pointing to the new plugin classes to load
   */
  override def addURL(url: URL): Unit = {
    if (!this.getURLs.contains(url)) super.addURL(url)
  }
}
