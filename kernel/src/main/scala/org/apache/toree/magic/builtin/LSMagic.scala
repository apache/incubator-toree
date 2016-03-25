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

import java.io.PrintStream

import org.apache.toree.magic._
import org.apache.toree.magic.dependencies.IncludeOutputStream
import org.apache.toree.plugins.annotations.Event

class LSMagic extends LineMagic with IncludeOutputStream {

  private def printStream = new PrintStream(outputStream)

  /**
   * Lists all available magics.
   * @param code The single line of code
   * @return The output of the magic
   */
  @Event(name = "lsmagic")
  override def execute(code: String): Unit = {
    val classes = new BuiltinLoader().loadClasses().toList
    val lineMagics = magicNames("%", classOf[LineMagic], classes)
      .mkString(" ").toLowerCase
    val cellMagics = magicNames("%%", classOf[CellMagic], classes)
      .mkString(" ").toLowerCase
    val message =
      s"""|Available line magics:
           |$lineMagics
           |
           |Available cell magics:
           |$cellMagics
           |
           |Type %<magic_name> for usage info.
         """.stripMargin

    printStream.println(message)
  }

  /**
   * Provides a list of class names from the given list that implement
   * the specified interface, with the specified prefix prepended.
   * @param prefix prepended to each name, e.g. "%%"
   * @param interface a magic interface, e.g. classOf[LineMagic]
   * @param classes a list of magic classes
   * @return list of class names with prefix
   */
  protected[magic] def magicNames(prefix: String, interface: Class[_],
                                  classes: List[Class[_]]) : List[String] = {
    val filteredClasses = classes.filter(_.getInterfaces.contains(interface))
    filteredClasses.map(c => s"${prefix}${c.getSimpleName}")
  }
}
