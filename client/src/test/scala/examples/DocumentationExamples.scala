package examples

import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.kernel.protocol.v5.client.SparkKernelClientBootstrap
import com.ibm.spark.kernel.protocol.v5.content._
import com.typesafe.config.{Config, ConfigFactory}

object DocumentationExamples extends App {
  /************
  *   Setup   *
  ************/
  //  Create the connection information
  val profileJSON: String = """
  {
      "stdin_port" : 48691,
      "control_port" : 44808,
      "hb_port" : 49691,
      "shell_port" : 40544,
      "iopub_port" : 43462,
      "ip" : "192.168.44.44",
      "transport" : "tcp",
      "signature_scheme" : "hmac-sha256",
      "key" : ""
  }
  """.stripMargin
  val config: Config = ConfigFactory.parseString(profileJSON)
  val client = new SparkKernelClientBootstrap(config).createClient

  /********************
  *   Executing Code  *
  ********************/
  //  Create a variable, z, and assign a value to it
  client.execute("val z = 0")
  //  Perform some computation
  client.execute("1 + 1")
  //  Print some message
  client.execute("println(\"Hello, World\")")

  /**********************************
  *   Receiving Results onResult    *
  **********************************/
  def printResult(result: ExecuteResult) = {
    println(s"Result was: ${result.data.get(MIMEType.PlainText).get}")
  }
  //  Create a variable, z, and assign a value to it
  client.execute("val z = 0").onResult(printResult)
  //  Perform some computation, and print it twice
  client.execute("1 + 1").onResult(printResult).onResult(printResult)
  //  The callback will never be invoked
  client.execute("someUndefinedVariable").onResult(printResult)

  /****************************************
  *   Receiving Print Streams onStream    *
  ****************************************/
  def printStreamContent(content:StreamContent) = {
    println(s"Stream content was: ${content.text}")
  }
  client.execute("println(1/1)").onStream(printStreamContent)
  client.execute("println(\"Hello, World\")").onStream(printStreamContent)

  /********************************
  *   Handling Errors: onError    *
  ********************************/
  def printError(reply:ExecuteReplyError) = {
    println(s"Error was: ${reply.ename.get}")
  }
  //  Error from executing a statement
  client.execute("1/0").onError(printError)
  //  Error from invoking a println
  client.execute("println(someUndefinedVar").onError(printError)
  /*
    Output should be:
      Result was: z: Int = 0
      Result was: res*: Int = 2
      Result was: res*: Int = 2
      Stream content was: 1

      Stream content was: Hello, World

      Error was: java.lang.ArithmeticException
      Error was: Syntax Error.
   */

  //  Wait for all the executions to complete
  Thread.sleep(5000)
  //  Properly shutdown the client and exit the application
  client.shutdown()
}
