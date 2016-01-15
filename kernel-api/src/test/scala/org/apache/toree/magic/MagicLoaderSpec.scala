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

package org.apache.toree.magic

import java.io.OutputStream

import org.apache.toree.dependencies.DependencyDownloader
import org.apache.toree.interpreter.Interpreter
import org.apache.toree.magic.dependencies._
import org.apache.spark.SparkContext
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}


/**
* Used for verification of dependency injection. Calls toString on each
* dependency to assert that they are provided.
*/
class LineMagicWithDependencies extends LineMagic
  with IncludeDependencyDownloader
  with IncludeSparkContext
  with IncludeInterpreter
  with IncludeOutputStream
{
  override def execute(code: String): Unit = {
    sparkContext.cancelAllJobs()
    interpreter.classServerURI
    outputStream.close()
    dependencyDownloader.setPrintStream(null)
  }
}

class MockLineMagic extends LineMagic {
  override def execute(code: String): Unit = {}
}

class MockCellMagic extends CellMagic {
  override def execute(code: String): CellMagicOutput = 
    CellMagicOutput()
}

class MagicLoaderSpec extends FunSpec with Matchers with MockitoSugar {
  describe("MagicLoader") {
    describe("#hasLineMagic") {
      it("should return false if a class with the magic name is not found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] =
            throw new ClassNotFoundException()
        }

        magicLoader.hasLineMagic("potato") should be (false)
      }

      it("should return true if a class with the magic name is found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] = 
            classOf[MockLineMagic]
        }

        magicLoader.hasLineMagic("potato") should be (true)
      }

      it("should return true if a class with the magic name is found regardless of case"){
        // Only loads a class named "Potato"
        val classLoader = new ClassLoader() {
          override def findClass(name: String) =
            if (name == "Potato") classOf[MockLineMagic]
            else throw new ClassNotFoundException
        }

        // Case insensitive matching should be performed on "Potato"
        val magicLoader = new MagicLoader(parentLoader = classLoader) {
          override def magicClassNames = List("Potato")
        }

        magicLoader.hasLineMagic("Potato") should be (true)
        magicLoader.hasLineMagic("potato") should be (true)
        magicLoader.hasLineMagic("pOTatO") should be (true)
      }
    }

    describe("#hasCellMagic") {
      it("should return false if a class with the magic name is not found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] =
            throw new ClassNotFoundException()
        }

        magicLoader.hasCellMagic("potato") should be (false)
      }

      it("should return true if a class with the magic name is found") {
        val magicLoader = new MagicLoader() {
          override def findClass(name: String): Class[_] =
            classOf[MockCellMagic]
        }

        magicLoader.hasCellMagic("potato") should be (true)
      }

      it("should return true if a class with the magic name is found regardless of case"){
        // Only loads a class named "Potato"
        val classLoader = new ClassLoader() {
          override def findClass(name: String) =
            if (name == "Potato") classOf[MockCellMagic]
            else throw new ClassNotFoundException
        }

        // Case insensitive matching should be performed on "Potato"
        val magicLoader = new MagicLoader(parentLoader = classLoader) {
          override def magicClassNames = List("Potato")
        }

        magicLoader.hasCellMagic("Potato") should be (true)
        magicLoader.hasCellMagic("potato") should be (true)
        magicLoader.hasCellMagic("pOTatO") should be (true)
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

    describe("#createMagicInstance") {
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

        val magicName = "LineMagicWithDependencies"
        val instance = magicLoader.createMagicInstance(magicName)
          .asInstanceOf[LineMagicWithDependencies]
        instance.interpreter should be(mockInterpreter)
        instance.outputStream should be(mockOutputStream)
        instance.sparkContext should be(mockSparkContext)
        instance.dependencyDownloader should be(mockDependencyDownloader)
      }
    }
  }
}
