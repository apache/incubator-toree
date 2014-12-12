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

package com.ibm.spark.magic.builtin

import com.google.common.reflect.ClassPath
import com.ibm.spark.magic.InternalClassLoader
import com.google.common.base.Strings._
import scala.collection.JavaConversions._

/**
 * Represents a class loader that loads classes from the builtin package.
 */
class BuiltinLoader
  extends InternalClassLoader(classOf[BuiltinLoader].getClassLoader) {

  private val pkgName = this.getClass.getPackage.getName
  
  def getClasses(pkg: String = pkgName) = {
    isNullOrEmpty(pkg) match {
      case true =>
        List()
      case false =>
        // TODO: Decide if this.getClass.getClassLoader should just be this
        val classPath = ClassPath.from(this.getClass.getClassLoader)
        classPath.getTopLevelClasses(pkg).filter(
          _.getSimpleName != this.getClass.getSimpleName
        )
    }
  }
}
