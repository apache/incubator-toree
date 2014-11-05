package examples

import java.io.File
import com.ibm.spark.kernel.protocol.v5.client.SparkKernelClientBootstrap
import com.typesafe.config.{ConfigFactory, Config}

/**
 * This App demonstrates how to use the spark client in scala.
 * Use this class as a playground.
 */
object ScalaSparkClientUsage extends App {
  val profile: File = new File(getClass.getResource("/kernel-profiles/IOPubIntegrationProfile.json").toURI)
  val config: Config = ConfigFactory.parseFile(profile)

  val client = new SparkKernelClientBootstrap(config).createClient

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
