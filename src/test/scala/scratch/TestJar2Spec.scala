package scratch

import java.net.URLClassLoader

import org.scalatest.{FunSpec, Matchers}

class TestJar2Spec extends FunSpec with Matchers {
  describe("TestJar2") {
    it("should import and run") {
      // Resource located in src/test/resources/TestJar2.jar
      //
      // Jar created via `jar -cf TestJar.jar com/ibm/testjar2/TestClass.class`
      //
      val testJar = this.getClass.getClassLoader.getResource("TestJar2.jar")

      // Resource contains a single class
      val clazz = new URLClassLoader(
        List(testJar).toArray,
        this.getClass.getClassLoader
      ).loadClass("com.ibm.testjar2.TestClass")

      // Resource class contains a single method
      val method = clazz.getDeclaredMethod("CallMe")

      // Example of executing it
      method.invoke(clazz.newInstance()) should be (3)
    }
  }
}
