package examples;
import com.ibm.spark.kernel.client.java.SparkKernelClient;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult;
import scala.collection.immutable.Map;
import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

/**
 * This main class demonstrates how to use the spark client in java.
 * Use this class as a playground.
 */
public class JavaSparkClientUsage {

    public static void main(String[] args) throws Exception {
        com.ibm.spark.client.SparkKernelClient scalaClient = new com.ibm.spark.client.SparkKernelClient();
        SparkKernelClient client = new SparkKernelClient(scalaClient);

        Thread.sleep(100); // actor system takes a moment to initialize

        client.heartbeat(
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("java hb success");
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("java hb error");
                }
            });

        client.execute(
            "val y = 9",
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("java exec success");
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("java exec error");
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("java exec result");
                }
            });
    }
}