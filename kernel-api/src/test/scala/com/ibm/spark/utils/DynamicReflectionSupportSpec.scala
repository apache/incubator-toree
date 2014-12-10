package com.ibm.spark.utils

import java.io.OutputStream

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{GivenWhenThen, BeforeAndAfter, FunSpec, Matchers}

class DynamicReflectionSupportSpec
  extends FunSpec with Matchers with MockitoSugar {

  describe("DynamicReflectionSupport") {
    describe("with a class instance") {
      describe("#selectDynamic") {
        it("should support accessing a normal field") {
          class MyTestClass {
            val test = 3
          }

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          dynamicSupport.test should be (3)
        }

        it("should support accessing a method with no arguments") {
          class MyTestClass {
            def test = 3
          }

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          dynamicSupport.test should be (3)
        }

        it("should throw an error if the field does not exist") {
          class MyTestClass

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          intercept[NoSuchFieldException] {
            dynamicSupport.test
          }
        }
      }

      describe("#applyDynamic") {
        it("should support executing a method with one argument") {
          class MyTestClass {
            def test(x: Int) = x
          }

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          dynamicSupport.test(5) should be (5)
        }

        it("should support executing a method with multiple arguments") {
          class MyTestClass {
            def test(x: Int, y: String) = (x, y)
          }

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          dynamicSupport.test(5, "test me") should be ((5, "test me"))
        }

        it("should throw an error if the method does not exist") {
          class MyTestClass

          val x: MyTestClass = new MyTestClass

          val dynamicSupport = DynamicReflectionSupport(x.getClass, x)

          intercept[NoSuchMethodException] {
            dynamicSupport.test(5, "test me")
          }
        }
      }
    }

    describe("with an object") {
      describe("#selectDynamic") {
        it("should support accessing a normal field") {
          object MyTestObject {
            val test = 3
          }

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          dynamicSupport.test should be (3)
        }

        it("should support accessing a method with no arguments") {
          object MyTestObject {
            def test = 3
          }

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          dynamicSupport.test should be (3)
        }

        it("should throw an error if the field does not exist") {
          object MyTestObject

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          intercept[NoSuchFieldException] {
            dynamicSupport.test
          }
        }
      }

      describe("#applyDynamic") {
        it("should support executing a method with one argument") {
          object MyTestObject {
            def test(x: Int) = x
          }

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          dynamicSupport.test(5) should be (5)
        }

        it("should support executing a method with multiple arguments") {
          object MyTestObject {
            def test(x: Int, y: String) = (x, y)
          }

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          dynamicSupport.test(5, "test me") should be ((5, "test me"))

        }

        it("should throw an error if the method does not exist") {
          object MyTestObject

          val dynamicSupport =
            DynamicReflectionSupport(MyTestObject.getClass, MyTestObject)

          intercept[NoSuchMethodException] {
            dynamicSupport.test(5, "test me")
          }
        }
      }
    }
  }
}
