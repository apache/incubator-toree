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

package com.ibm.spark.magic

import java.io.OutputStream

import com.ibm.spark.dependencies.DependencyDownloader
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.magic.dependencies._
import org.apache.spark.SparkContext
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class MagicWithNoDependencies extends MagicTemplate {
  override def executeLine(code: String): MagicOutput = MagicOutput()
  override def executeCell(code: Seq[String]): MagicOutput = MagicOutput()
}

/**
 * Used for verification of dependency injection. Calls toString on each
 * dependency to assert that they are provided.
 */
class MagicWithDependencies extends MagicTemplate
  with IncludeDependencyDownloader
  with IncludeSparkContext
  with IncludeInterpreter
  with IncludeOutputStream
{
  override def executeLine(code: String): MagicOutput = {
    sparkContext.cancelAllJobs()
    interpreter.classServerURI
    outputStream.close()
    dependencyDownloader.setPrintStream(null)
    MagicOutput()
  }
  override def executeCell(code: Seq[String]): MagicOutput = {
    sparkContext.cancelAllJobs()
    interpreter.classServerURI
    outputStream.close()
    dependencyDownloader.setPrintStream(null)
    MagicOutput()
  }
}

class MagicLoaderSpec extends FunSpec with Matchers with MockitoSugar {
  describe("MagicLoader") {
    describe("#hasMagic") {
      it("should return false if a class with the magic name is not found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] =
            throw new ClassNotFoundException()
        }

        magicLoader.hasMagic("potato") should be (false)
      }

      it("should return true if a class with the magic name is found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] = this.getClass
        }

        magicLoader.hasMagic("potato") should be (true)
      }

      it("should return true if a class with the magic name is found irregardless of case"){

        // Only loads a class named "Potato"
        val classLoader = new ClassLoader() {
          override def findClass(name: String) =
            if (name == "Potato") this.getClass
            else throw new ClassNotFoundException
        }

        // Case insensitive matching should be performed on "Potato"
        val magicLoader = new MagicLoader(parentLoader = classLoader) {
          override def magicClassNames = List("Potato")
        }

        magicLoader.hasMagic("Potato") should be (true)
        magicLoader.hasMagic("potato") should be (true)
        magicLoader.hasMagic("pOTatO") should be (true)
      }
    }

    describe("#magicClassName"){
      it("should return the correctly-cased version of the requested magic name") {
        val magicLoader = new MagicLoader() {
          override def magicClassNames = List("Potato")
        }

        magicLoader.magicClassName("Potato") should be ("Potato")
        magicLoader.magicClassName("potato") should be ("Potato")
        magicLoader.magicClassName("pOTatO") should be ("Potato")
      }

      it("should return the query if a corresponding magic class does not exist") {
        val magicLoader = new MagicLoader() {
          override def magicClassNames = List()
        }

        magicLoader.magicClassName("dne") should be ("dne")
        magicLoader.magicClassName("dNE") should be ("dNE")
      }
    }

    describe("#executeMagic") {
      it("should execute the line magic if isCell == false") {
        val name = "potato"
        val code = "cheese"
        val isCell = false

        val mockMagic = mock[MagicTemplate]

        val magicLoader = new MagicLoader() {
          override protected def createMagicInstance(name: String): Any =
            mockMagic
        }

        magicLoader.executeMagic(name, code, isCell)
        verify(mockMagic).executeLine(code)
      }

      it("should execute the cell magic if isCell == true") {
        val name = "potato"
        val code = Seq("cheese", "is", "delicious")
        val isCell = true

        val mockMagic = mock[MagicTemplate]

        val magicLoader = new MagicLoader() {
          override protected def createMagicInstance(name: String): Any =
            mockMagic
        }

        magicLoader.executeMagic(name, code.mkString("\n"), isCell)
        verify(mockMagic).executeCell(code)
      }

      it("should correctly load a class to execute") {
        val dependencyMap = new DependencyMap()

        val magicLoader = new MagicLoader(
          dependencyMap = dependencyMap,
          parentLoader = new InternalClassLoader(getClass.getClassLoader)
        )

        val magicName = "MagicWithNoDependencies"
        magicLoader.executeMagic(magicName, "", false) should be (MagicOutput())
      }

      it("should correctly insert dependencies into a class") {
        val mockInterpreter = mock[Interpreter]
        val mockSparkContext = mock[SparkContext]
        val mockOutputStream = mock[OutputStream]
        val mockDependencyDownloader = mock[DependencyDownloader]

        val dependencyMap = new DependencyMap()
          .setInterpreter(mockInterpreter)
          .setSparkContext(mockSparkContext)
          .setOutputStream(mockOutputStream)
          .setDependencyDownloader(mockDependencyDownloader)

        val magicLoader = new MagicLoader(
          dependencyMap = dependencyMap,
          parentLoader = new InternalClassLoader(getClass.getClassLoader)
        )

        val magicName = "MagicWithDependencies"
        magicLoader.executeMagic(magicName, "", false) should be (MagicOutput())

        verify(mockInterpreter).classServerURI
        verify(mockSparkContext).cancelAllJobs()
        verify(mockOutputStream).close()
        verify(mockDependencyDownloader).setPrintStream(null)
      }
    }
  }

}
