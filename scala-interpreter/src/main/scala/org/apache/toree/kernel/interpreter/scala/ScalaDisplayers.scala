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

package org.apache.toree.kernel.interpreter.scala

import java.util

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try
import org.apache.spark.SparkContext
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import jupyter.Displayer
import jupyter.Displayers
import jupyter.MIMETypes
import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.MagicOutput

object ScalaDisplayers {

  // This method is called to ensure this class is loaded. When loaded, the display methods below
  // are registered with jupyter.Displayers and will get used to display Scala and Java objects.
  def ensureLoaded(): Unit = ()

  private def toJava(body: => Map[String, String]): util.Map[String, String] = {
    body.asJava
  }

  Displayers.register(classOf[MagicOutput], new Displayer[MagicOutput] {
    override def display(data: MagicOutput): util.Map[String, String] = toJava {
      data.asMap
    }
  })

  Displayers.register(classOf[SparkContext], new Displayer[SparkContext] {
    override def display(sc: SparkContext): util.Map[String, String] = toJava {
      val appId = sc.applicationId
      val html =
        s"""<ul>
           |<li><a href="${sc.uiWebUrl}" target="new_tab">Spark UI: $appId</a></li>
           |</ul>""".stripMargin
      val text =
        s"""Spark $appId: ${sc.uiWebUrl}"""

      Map(
        MIMEType.PlainText -> text,
        MIMEType.TextHtml -> html
      )
    }
  })

  Displayers.register(classOf[Array[Row]], new Displayer[Array[Row]] {
    override def display(arr: Array[Row]): util.Map[String, String] = toJava {
      val (text, html) = displayRows(arr)
      Map(MIMEType.PlainText -> text, MIMEType.TextHtml -> html)
    }
  })

  Displayers.register(classOf[Option[_]], new Displayer[Option[_]] {
    override def display(option: Option[_]): util.Map[String, String] = toJava {
      val result = new mutable.HashMap[String, String]

      option match {
        case Some(wrapped) =>
          Displayers.display(wrapped).asScala.foreach {
            case (mime, text) if mime == MIMETypes.TEXT =>
              result.put(mime, "Some(" + text + ")")
            case (mime, value) =>
              result.put(mime, value)
          }
        case None =>
          result.put(MIMETypes.TEXT, "None")
      }

      result.toMap
    }
  })

  Displayers.register(classOf[SparkSession], new Displayer[SparkSession] {
    override def display(spark: SparkSession): util.Map[String, String] = {
      Displayers.display(spark.sparkContext)
    }
  })

  // Set the default displayer to call toHtml if present on Scala objects
  Displayers.registration.setDefault(new Displayer[Object] {
    override def display(obj: Object): util.Map[String, String] = toJava {
      if (obj.getClass.isArray) {
        Map(MIMETypes.TEXT -> obj.asInstanceOf[Array[_]].map(
          elem => Displayers.display(elem).get(MIMETypes.TEXT)
        ).mkString("[", ", ", "]"))
      } else {
        val objAsString = String.valueOf(obj)
        Try(callToHTML(obj)).toOption.flatten match {
          case Some(html) =>
            Map(
              MIMETypes.TEXT -> objAsString,
              MIMETypes.HTML -> html
            )
          case None =>
            Map(MIMETypes.TEXT -> objAsString)
        }
      }
    }

    private def callToHTML(obj: Any): Option[String] = {
      import scala.reflect.runtime.{universe => ru}
      val toHtmlMethodName = ru.TermName("toHtml")
      val classMirror = ru.runtimeMirror(obj.getClass.getClassLoader)
      val objMirror = classMirror.reflect(obj)
      val toHtmlSym = objMirror.symbol.toType.member(toHtmlMethodName)
      if (toHtmlSym.isMethod) {
        Option(String.valueOf(objMirror.reflectMethod(toHtmlSym.asMethod).apply()))
      } else {
        None
      }
    }
  })

  private def displayRows(
                   rows: Array[Row],
                   fields: Option[Seq[String]] = None,
                   isTruncated: Boolean = false): (String, String) = {
    if (rows.length < 1) {
      return ("", "")
    }

    val lengths = Array.fill(rows(0).length)(3)
    val cells = rows.map { row =>
      row.toSeq.zipWithIndex.map {
        case (value, pos) =>
          val repr = value match {
            case null => "NULL"
            case binary: Array[Byte] =>
              binary.map("%02X".format(_)).mkString("[", " ", "]")
            case arr: Array[_] =>
              arr.mkString("[", ", ", "]")
            case seq: Seq[_] =>
              seq.mkString("[", ", ", "]")
            case map: Map[_, _] =>
              map.map {
                case (k: Any, v: Any) => s"$k -> $v"
              }.mkString("{", ", ", "}")
            case _ =>
              value.toString
          }
          lengths(pos) = Math.max(lengths(pos), repr.length)
          repr
      }
    }

    fields match {
      case Some(names) =>
        names.zipWithIndex.foreach {
          case (name, pos) =>
            lengths(pos) = Math.max(lengths(pos), name.length)
        }
      case _ =>
    }

    var htmlLines = new mutable.ArrayBuffer[String]()
    htmlLines
    htmlLines += "<table>"

    var lines = new mutable.ArrayBuffer[String]()
    val divider = lengths.map(l => "-" * (l + 2)).mkString("+", "+", "+")
    val format = lengths.map(l => s" %-${l}s ").mkString("|", "|", "|")
    lines += divider

    fields match {
      case Some(names) =>
        htmlLines += names.mkString("<tr><th>", "</th><th>", "</th></tr>")
        lines += String.format(format, names:_*)
        lines += divider
      case _ =>
    }

    cells.foreach { row =>
      htmlLines += row.mkString("<tr><td>", "</td><td>", "</td></tr>")
      lines += String.format(format, row: _*)
    }

    if (isTruncated) {
      val dots = Array.fill(lengths.length)("...")
      htmlLines += dots.mkString("<tr><td>", "</td><td>", "</td></tr>")
      lines += String.format(format, dots: _*)
    }

    htmlLines += "</table>"
    lines += divider

    (lines.mkString("\n"), htmlLines.mkString("\n"))
  }
}
