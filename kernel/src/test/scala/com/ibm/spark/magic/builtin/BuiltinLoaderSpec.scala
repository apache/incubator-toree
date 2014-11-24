package com.ibm.spark.magic.builtin

import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FunSpec}

class BuiltinLoaderSpec extends FunSpec with Matchers with MockitoSugar {
  describe("BuiltinLoader") {
    describe("#getClasses") {
      it("should return classes in a package") {
        val pkg = this.getClass.getPackage.getName
        val classes = new BuiltinLoader().getClasses(pkg)
        classes.size shouldNot be(0)
      }
    }
  }
}
