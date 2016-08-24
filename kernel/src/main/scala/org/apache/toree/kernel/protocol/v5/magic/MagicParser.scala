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

package org.apache.toree.kernel.protocol.v5.magic

import org.apache.toree.magic.MagicManager

import scala.util.Try

class MagicParser(private val magicManager: MagicManager) {
  private val magicRegex = """^[%]{1,2}(\w*)""".r
  protected[magic] val kernelObjectName = "kernel.magics"

  /**
   * Determines whether a given line of code represents a line magic.
   *
   * @param codeLine a single line of code
   * @return
   */
  private def isLineMagic(codeLine: String) = codeLine.startsWith("%") &&
    !isCellMagic(codeLine)

  /**
   * Determines whether a given string of code represents a cell magic.
   *
   * @param codeBlob a string of code separated by newlines
   * @return
   */
  private def isCellMagic(codeBlob: String) = codeBlob.startsWith("%%")

  /**
   * Finds the first occurrence of a "magic string" i.e. "%%magic" or "%magic"
   * in a given code string, and separates the magic name from the code that
   * follows it.
   *
   * E.g.
   * "%magic foo bar" -> ("magic", "foo bar")
   *
   * @param codeBlob a string of code separated by newlines
   * @return (magicName, args)
   */
  protected[magic] def parseMagic(codeBlob: String): Option[(String, String)] = {
    val matchData =
      magicRegex.findFirstMatchIn(codeBlob)

    matchData match {
      case Some(m) => Some((m.group(1), m.after(1).toString.trim))
      case None => None
    }
  }

  /**
   * Given a line of code representing a magic invocation determines whether
   * the magic has an implementation.
   *
   * @param codeLine a single line of code
   * @return true if the magic exists and is a line magic
   */
  protected[magic] def isValidLineMagic(codeLine: String): Boolean = {
    parseMagic(codeLine) match {
      case Some((magicName, _)) =>
        isLineMagic(codeLine) && Try(magicManager.isLineMagic(
          magicManager.findMagic(magicName))
        ).getOrElse(false)
      case None => false
    }
  }

  /**
   * Given a blob of code, finds any magic invocations of magics that don't
   * exist.
   *
   * @param codeBlob a string of code separated by newlines
   * @return invalid magic names from the given code blob
   */
  protected[magic] def parseOutInvalidMagics(codeBlob: String): List[String] = {
    val lineMagics = codeBlob.split("\n").toList.filter(isLineMagic)
    lineMagics.filterNot(isValidLineMagic).map(line => {
      val (magicName, _) = parseMagic(line).get
      magicName
    })
  }

  /**
   * Formats a given magic name and args to code for a kernel method call.
   *
   * @param magicName the name of the magic
   * @param args the arguments to the magic
   * @return equivalent kernel method call
   */
  protected[magic] def substitute(magicName: String, args: String): String =
    s"""$kernelObjectName.$magicName(\"\"\"$args\"\"\")"""

  /**
   * Formats a given line of code representing a line magic invocation into an
   * equivalent kernel object call if the magic invocation is valid.
   *
   * @param codeLine the line of code to convert.
   * @return a substituted line of code if valid else the original line
   */
  protected[magic] def substituteLine(codeLine: String): String = {
    isValidLineMagic(codeLine) match {
      case true =>
        val (magicName, args) = parseMagic(codeLine).get
        substitute(magicName, args)
      case false => codeLine
    }
  }

  /**
   * Formats a given code blob representing a cell magic invocation into an
   * equivalent kernel object call if the cell magic invocation is valid. An
   * error message is returned if not.
   *
   * @param codeBlob the blob of code representing a cell magic invocation
   * @return Left(the substituted code) or Right(error message)
   */
  protected[magic] def parseCell(codeBlob: String): Either[String, String] = {
    parseMagic(codeBlob.trim) match {
      case Some((cellMagicName, args)) =>
        val m = Try(magicManager.findMagic(cellMagicName))
        m.map(magicManager.isCellMagic).getOrElse(false) match {
          case true => Left(substitute(cellMagicName, args))
          case false => Right(s"Magic $cellMagicName does not exist!")
        }
      case None => Left(codeBlob)
    }
  }

  /**
   * Parses all lines in a given code blob and either substitutes equivalent
   * kernel object calls for each line magic in the code blob OR returns
   * an error message if any of the line magic invocations were invalid.
   *
   * @param codeBlob a string of code separated by newlines
   * @return Left(code blob with substitutions) or Right(error message)
   */
  protected[magic] def parseLines(codeBlob: String): Either[String, String] = {
    val invalidMagics = parseOutInvalidMagics(codeBlob.trim)
    invalidMagics match {
      case Nil =>
        val substitutedCode = codeBlob.trim.split("\n").map(substituteLine)
        Left(substitutedCode.mkString("\n"))
      case _ =>
        Right(s"Magics [${invalidMagics.mkString(", ")}] do not exist!")
    }
  }

  /**
   * Parses a given code blob and returns an equivalent blob with substitutions
   * for magic invocations, if any, or an error string.
   *
   * @param codeBlob the blob of code to parse
   * @return Left(parsed code) or Right(error message)
   */
  def parse(codeBlob: String): Either[String, String] = {
    val trimCodeBlob = codeBlob.trim
    isCellMagic(trimCodeBlob) match {
      case true => parseCell(trimCodeBlob)
      case false => parseLines(trimCodeBlob)
    }
  }
}
