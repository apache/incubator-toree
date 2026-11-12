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

package org.apache.toree.boot.layer

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class InterruptSignalHandlerSpec extends AnyFunSpec with Matchers
  with TableDrivenPropertyChecks
{
  private val Window = 3000L

  /** Records calls made by the handler; time is driven by the test. */
  private class Fixture(interruptFails: Boolean = false) {
    var now = 1000000L
    var interrupts = 0
    var shutdowns = 0

    val handler = new InterruptSignalHandler(
      interrupt = () => {
        interrupts += 1
        if (interruptFails) throw new IllegalStateException("interrupt failed")
      },
      shutdown = () => shutdowns += 1,
      shutdownWindowMillis = Window,
      clock = () => now
    )

    /** Delivers SIGINTs at the given offsets (ms) from the start time. */
    def signalAt(offsets: Long*): Unit = {
      val start = now
      offsets.foreach { offset => now = start + offset; handler.onSignal() }
    }
  }

  describe("InterruptSignalHandler") {
    describe("#onSignal") {
      val cases = Table(
        ("description", "offsets", "interrupts", "shutdowns"),
        ("interrupts on the first signal", Seq(0L), 1, 0),
        ("shuts down on a second signal within the window", Seq(0L, 500L), 1, 1),
        ("interrupts again when the second signal is outside the window",
          Seq(0L, Window + 1), 2, 0),
        ("shuts down on the boundary of the window", Seq(0L, Window), 1, 1)
      )

      forAll(cases) { (description, offsets, interrupts, shutdowns) =>
        it(s"should behave correctly: $description") {
          val f = new Fixture()
          f.signalAt(offsets: _*)
          f.interrupts should be (interrupts)
          f.shutdowns should be (shutdowns)
        }
      }

      it("should not propagate a failure raised by interrupt") {
        val f = new Fixture(interruptFails = true)
        noException should be thrownBy f.signalAt(0L)
        f.interrupts should be (1)
      }

      it("should still shut down on a double signal when interrupt fails") {
        // Regression: the timestamp used to be recorded only after a
        // successful interrupt, so a failing interrupt (e.g. Spark failing
        // to initialize in the signal thread on JDK 11+) blocked shutdown.
        val f = new Fixture(interruptFails = true)
        f.signalAt(0L, 500L)
        f.shutdowns should be (1)
      }
    }

    describe("#onAlternateSignal") {
      it("should only interrupt, never shut down, however often it fires") {
        val f = new Fixture()
        (1 to 3).foreach { _ => f.handler.onAlternateSignal() }
        f.interrupts should be (3)
        f.shutdowns should be (0)
      }

      it("should not propagate a failure raised by interrupt") {
        val f = new Fixture(interruptFails = true)
        noException should be thrownBy f.handler.onAlternateSignal()
      }
    }
  }
}
