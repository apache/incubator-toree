package examples

import com.ibm.spark.SparkKernelClientBootstrap
import com.ibm.spark.client.SparkKernelClientOptions

/**
 * This App demonstrates how to use the spark client in scala.
 * Use this class as a playground.
 */
object ScalaSparkClientUsage extends App {
  val options: SparkKernelClientOptions = new SparkKernelClientOptions(args)
  val client = new SparkKernelClientBootstrap(options.toConfig).createClient

  Thread.sleep(100) // actor system takes a moment to initialize
  
  client.heartbeat(() => {
      println("hb bad")
  })
  client.submit("val z = 0")

  val func = (x: Any) => println(x)
  val code: String =
    """
      val s = new Thread(new Runnable {
        def run() {
          while(true) {
            Thread.sleep(1000)
            println("bar")
          }
        }
      })
      s.start()
    """.stripMargin

  client.stream(code, func)
  Thread.sleep(100000)
}
