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
package org.apache.toree.dependencies

import java.net.URL
import java.nio.file.Files

import org.scalatest.{FunSpec, Matchers, OneInstancePerTest}

class CoursierDependencyDownloaderSpec extends FunSpec with Matchers
  with OneInstancePerTest
{
  private val coursierDependencyDownloader = new CoursierDependencyDownloader

  describe("CoursierDependencyDownloader") {
    describe("#addMavenRepository") {
      it("should add to the list of repositories") {
        val repo = new URL("http://some-repo.com")

        coursierDependencyDownloader.addMavenRepository(repo, None)

        val repos = coursierDependencyDownloader.getRepositories

        repos should contain (repo.toURI)
      }
    }

    describe("#removeMavenRepository") {
      it("should remove from the list of repositories") {
        val repo = new URL("http://some-repo.com")

        coursierDependencyDownloader.addMavenRepository(repo, None)
        coursierDependencyDownloader.removeMavenRepository(repo)

        val repos = coursierDependencyDownloader.getRepositories

        repos should not contain (repo.toURI)
      }
    }

    describe("#setDownloadDirectory") {
      it("should set the new download directory if valid") {
        val validDir = Files.createTempDirectory("tmpdir").toFile
        validDir.deleteOnExit()

        val result = coursierDependencyDownloader.setDownloadDirectory(validDir)
        result should be (true)

        val dir = coursierDependencyDownloader.getDownloadDirectory
        dir should be (validDir.getAbsolutePath)
      }

      it("should not change the directory if given a file") {
        val invalidDir = Files.createTempFile("tmp", "file").toFile
        invalidDir.deleteOnExit()

        val result = coursierDependencyDownloader.setDownloadDirectory(invalidDir)
        result should be (false)

        val dir = coursierDependencyDownloader.getDownloadDirectory
        dir should not be (invalidDir.getAbsolutePath)
      }

      it("should support creating missing directories") {
        val baseDir = Files.createTempDirectory("tmpdir").toFile
        val validDir = baseDir.toPath.resolve("otherdir").toFile
        validDir.deleteOnExit()
        baseDir.deleteOnExit()

        val result = coursierDependencyDownloader.setDownloadDirectory(validDir)
        result should be (true)

        val dir = coursierDependencyDownloader.getDownloadDirectory
        dir should be (validDir.getAbsolutePath)
      }
    }

    describe("#getRepositories") {
      it("should have the default repositories") {
        val expected = Seq(DependencyDownloader.DefaultMavenRepository.toURI)

        val actual = coursierDependencyDownloader.getRepositories

        actual should be (expected)
      }
    }

    describe("#getDownloadDirectory") {
      it("should have the default download directory") {
        val expected = DependencyDownloader.DefaultDownloadDirectory.getAbsolutePath

        val actual = coursierDependencyDownloader.getDownloadDirectory

        actual should be (expected)
      }
    }
  }
}
