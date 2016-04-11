package test

import scala.concurrent.duration._
import scala.util.Properties

package object utils {
  val TestDilation = Properties.envOrElse("TEST_DILATION", "1").toInt
  val MaxAkkaTestTimeout = (5 * TestDilation).seconds
  val MaxAkkaTestInterval = (100 * TestDilation).milliseconds

}
