package scratch

import java.net.URLClassLoader

import org.scalatest.{Matchers, FunSpec}

class TestJarSpec extends FunSpec with Matchers {
  describe("TestJar") {
    it("should import and run") {
      // Resource located in src/test/resources/TestJar.jar
      //
      // Jar created via `jar -cf TestJar.jar com/ibm/testjar/TestClass.class`
      //
      val testJar = this.getClass.getClassLoader.getResource("TestJar.jar")

      // Resource contains a single class
      val clazz = new URLClassLoader(
        List(testJar).toArray,
        this.getClass.getClassLoader
      ).loadClass("com.ibm.testjar.TestClass")

      // Resource class contains a single method
      val method = clazz.getDeclaredMethod("sayHello", classOf[String])

      // Example of executing it
      method.invoke(clazz.newInstance(), "Chip") should be ("Hello, Chip")
    }
  }
}
