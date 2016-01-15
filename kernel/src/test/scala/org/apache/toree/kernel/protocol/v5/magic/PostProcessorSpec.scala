package org.apache.toree.kernel.protocol.v5.magic

import org.apache.toree.interpreter.Interpreter
import org.apache.toree.kernel.protocol.v5._
import org.apache.toree.magic.{CellMagicOutput, LineMagicOutput}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class PostProcessorSpec extends FunSpec with Matchers with MockitoSugar{
  describe("#matchCellMagic") {
    it("should return the cell magic output when the Left contains a " +
       "CellMagicOutput") {
      val processor = new PostProcessor(mock[Interpreter])
      val codeOutput = "some output"
      val cmo = CellMagicOutput()
      val left = Left(cmo)
      processor.matchCellMagic(codeOutput, left) should be(cmo)
    }

    it("should package the original code when the Left does not contain a " +
       "CellMagicOutput") {
      val processor = new PostProcessor(mock[Interpreter])
      val codeOutput = "some output"
      val left = Left("")
      val data = Data(MIMEType.PlainText -> codeOutput)
      processor.matchCellMagic(codeOutput, left) should be(data)
    }
  }

  describe("#matchLineMagic") {
    it("should process the code output when the Right contains a " +
       "LineMagicOutput") {
      val processor = spy(new PostProcessor(mock[Interpreter]))
      val codeOutput = "some output"
      val lmo = LineMagicOutput
      val right = Right(lmo)
      processor.matchLineMagic(codeOutput, right)
      verify(processor).processLineMagic(codeOutput)
    }

    it("should package the original code when the Right does not contain a " +
       "LineMagicOutput") {
      val processor = new PostProcessor(mock[Interpreter])
      val codeOutput = "some output"
      val right = Right("")
      val data = Data(MIMEType.PlainText -> codeOutput)
      processor.matchLineMagic(codeOutput, right) should be(data)
    }
  }

  describe("#processLineMagic") {
    it("should remove the result of the magic invocation if it is the last " +
       "line") {
      val processor = new PostProcessor(mock[Interpreter])
      val x = "hello world"
      val codeOutput = s"$x\nsome other output"
      val data = Data(MIMEType.PlainText -> x)
      processor.processLineMagic(codeOutput) should be(data)
    }
  }

  describe("#process") {
    it("should call matchCellMagic when the last variable is a Left") {
      val intp = mock[Interpreter]
      val left = Left("")
      // Need to mock lastExecutionVariableName as it is being chained with
      // the read method
      doReturn(Some("")).when(intp).lastExecutionVariableName
      doReturn(Some(left)).when(intp).read(anyString())

      val processor = spy(new PostProcessor(intp))
      val codeOutput = "hello"
      processor.process(codeOutput)
      verify(processor).matchCellMagic(codeOutput, left)
    }

    it("should call matchLineMagic when the last variable is a Right") {
      val intp = mock[Interpreter]
      val right = Right("")
      // Need to mock lastExecutionVariableName as it is being chained with
      // the read method
      doReturn(Some("")).when(intp).lastExecutionVariableName
      doReturn(Some(right)).when(intp).read(anyString())

      val processor = spy(new PostProcessor(intp))
      val codeOutput = "hello"
      processor.process(codeOutput)
      verify(processor).matchLineMagic(codeOutput, right)
    }

    it("should package the original code output when the Left is not a " +
      "Left[CellMagicOutput, Nothing]") {
      val intp = mock[Interpreter]
      val left = Left("")
      // Need to mock lastExecutionVariableName as it is being chained with
      // the read method
      doReturn(Some("")).when(intp).lastExecutionVariableName
      doReturn(Some(left)).when(intp).read(anyString())

      val processor = spy(new PostProcessor(intp))
      val codeOutput = "hello"
      val data = Data(MIMEType.PlainText -> codeOutput)
      processor.process(codeOutput) should be(data)
    }

    it("should package the original code output when the Right is not a " +
       "Right[LineMagicOutput, Nothing]") {
      val intp = mock[Interpreter]
      val right = Right("")
      // Need to mock lastExecutionVariableName as it is being chained with
      // the read method
      doReturn(Some("")).when(intp).lastExecutionVariableName
      doReturn(Some(right)).when(intp).read(anyString())

      val processor = spy(new PostProcessor(intp))
      val codeOutput = "hello"
      val data = Data(MIMEType.PlainText -> codeOutput)
      processor.process(codeOutput) should be(data)
    }
  }
}
