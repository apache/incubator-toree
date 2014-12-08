package com.ibm.spark.magic

import org.scalatest.{Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar

class InternalClassLoaderSpec extends FunSpec with Matchers with MockitoSugar {

  abstract class MockClassLoader extends ClassLoader(null) {
    override def loadClass(name: String): Class[_] = null
  }

  describe("InternalClassLoader") {
    describe("#loadClass") {
      it("should invoke super loadClass with loader's package prepended") {
        val expected = classOf[Class[_]]
        val packageName = "com.ibm.spark.magic"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            parentLoadClassCorrectlyInvoked =
              name == s"$packageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(className, true) should be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }

      it("should use loader's package instead of provided package first") {
        val expected = classOf[Class[_]]
        val forcedPackageName = "com.ibm.spark.magic"
        val packageName = "some.other.package"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            parentLoadClassCorrectlyInvoked =
              name == s"$forcedPackageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(s"$packageName.$className", true) should be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }

      it("should invoke super loadClass with given package if internal missing") {
        val expected = classOf[Class[_]]
        val packageName = "some.other.package"
        val className = "SomeClass"

        var parentLoadClassCorrectlyInvoked = false

        var methodCalled = false
        val internalClassLoader = new InternalClassLoader(null) {
          override private[magic] def parentLoadClass(name: String, resolve: Boolean): Class[_] = {
            if (!methodCalled) {
              methodCalled = true
              throw new ClassNotFoundException()
            }

            parentLoadClassCorrectlyInvoked =
              name == s"$packageName.$className" && resolve
            expected
          }
        }

        internalClassLoader.loadClass(s"$packageName.$className", true) should
          be (expected)

        parentLoadClassCorrectlyInvoked should be (true)
      }
    }
  }
}
