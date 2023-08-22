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

import org.apache.toree.kernel.protocol.v5.MIMEType
import org.apache.toree.magic.CellMagicOutput
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class HtmlSpec extends AnyFunSpec with Matchers with MockitoSugar {
  describe("Html"){
    describe("#execute") {
      it("should return the entire cell's contents with the MIME type of " +
         "text/html") {
        val htmlMagic = new Html

        val code = "some code on a line\nanother line"
        val expected = CellMagicOutput(MIMEType.TextHtml -> code)
        htmlMagic.execute(code) should be (expected)
      }
    }
  }
}
