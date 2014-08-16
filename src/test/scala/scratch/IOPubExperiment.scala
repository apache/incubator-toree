package scratch

import com.ibm.spark.SparkKernelClientBootstrap
import com.ibm.spark.client.SparkKernelClientOptions
import com.ibm.spark.client.exception.ShellException
import com.ibm.spark.kernel.protocol.v5.content.{ExecuteReply, ExecuteResult}
import com.ibm.spark.utils.LogLike
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

/**
 * Created by Chris on 8/8/14.
 */
class IOPubExperiment extends FunSpec with Matchers with LogLike {
  describe("iopub") {
    it("should work") {
      val client = new SparkKernelClientBootstrap(new SparkKernelClientOptions(Seq())).createClient
      Thread.sleep(1000)
      val replyCallback = (message: ExecuteReply) =>   println("REPLY: " + Json.parse(message).toString())
      val resultCallback = (message: ExecuteResult) => println("RESULT: " + message.data.toString())
      val errCallback = (exception: ShellException) => println(exception.toString)
      client.execute("val x = 2", replyCallback, resultCallback, errCallback)
      client.execute("val x = 2", replyCallback, resultCallback, errCallback)
      Thread.sleep(2*1000)
    }
  }
}
