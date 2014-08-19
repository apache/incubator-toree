package examples

import com.ibm.spark.SparkKernelClientBootstrap
import com.ibm.spark.client.SparkKernelClientOptions

/**
 * This App demonstrates how to use the spark client in scala.
 * Use this class as a playground.
 */
object ScalaSparkClientUsage extends App {
  val options: SparkKernelClientOptions = new SparkKernelClientOptions(args)
  val client = new SparkKernelClientBootstrap(options).createClient

  Thread.sleep(100) // actor system takes a moment to initialize
  
  client.heartbeat(() => {
      println("hb bad")
  })
  client.submit("val z = 0")
}