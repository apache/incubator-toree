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

package org.apache.toree.magic.builtin

import com.google.common.reflect.ClassPath
import com.google.common.reflect.ClassPath.ClassInfo
import org.apache.toree.magic.InternalClassLoader
import com.google.common.base.Strings._
import scala.collection.JavaConverters._

/**
 * Represents a class loader that loads classes from the builtin package.
 */
class BuiltinLoader
  extends InternalClassLoader(classOf[BuiltinLoader].getClassLoader) {

  private val pkgName = this.getClass.getPackage.getName

  /**
   * Provides a list of ClassInfo objects for each class in the specified
   * package.
   * @param pkg package name
   * @return list of ClassInfo objects
   */
  def getClasses(pkg: String = pkgName): List[ClassInfo] = {
    isNullOrEmpty(pkg) match {
      case true =>
        List()
      case false =>
        // TODO: Decide if this.getClass.getClassLoader should just be this
        val classPath = ClassPath.from(this.getClass.getClassLoader)
        classPath.getTopLevelClasses(pkg).asScala.filter(
          _.getSimpleName != this.getClass.getSimpleName
        ).toList
    }
  }

  /**
   * Provides a list of Class[_] objects for each class in the specified
   * package.
   * @param pkg package name
   * @return list of Class[_] objects
   */
  def loadClasses(pkg: String = pkgName): List[Class[_]] =
    getClasses(pkg).map(_.load())
}
