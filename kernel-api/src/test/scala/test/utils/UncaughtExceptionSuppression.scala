package test.utils

trait UncaughtExceptionSuppression {
  Thread.setDefaultUncaughtExceptionHandler(
    new Thread.UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {}
    })
}
