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

package org.apache.toree.magic.dependencies

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.magic.Magic

trait IncludeInterpreter {
  this: Magic =>

  //val interpreter: Interpreter
  private var _interpreter: Interpreter = _
  def interpreter: Interpreter = _interpreter
  def interpreter_=(newInterpreter: Interpreter) =
    _interpreter = newInterpreter
}
