package examples

import com.ibm.spark.client.SparkKernelClient

/**
 * This App demonstrates how to use the spark client in scala.
 * Use this class as a playground.
 */
object ScalaSparkClientUsage extends App {

    val client = new SparkKernelClient()
  
    Thread.sleep(100) // actor system takes a moment to initialize
  
    client.heartbeat(() => {
      println("hb good")
    }, () => {
      println("hb bad")
    })
    client.execute("val z = 0",
      () => {
        println("exec good")
      },
      () => {
        println("exec bad")
      },
      () => {
        println("result")
      })
}