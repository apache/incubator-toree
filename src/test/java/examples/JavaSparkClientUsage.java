package examples;

import com.ibm.spark.SparkKernelClientBootstrap;
import com.ibm.spark.client.SparkKernelClientOptions;
import com.ibm.spark.client.exception.ShellException;
import com.ibm.spark.kernel.client.java.EmptyFunction;
import com.ibm.spark.kernel.client.java.Function;
import com.ibm.spark.kernel.client.java.SparkKernelClient;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult;

/**
 * This main class demonstrates how to use the spark client in java.
 * Use this class as a playground.
 */
public class JavaSparkClientUsage {

    public static void main(String[] args) throws Exception {
        SparkKernelClient client = new SparkKernelClient(
                new SparkKernelClientBootstrap(
                        new SparkKernelClientOptions(args)).createClient());

        Thread.sleep(100); // actor system takes a moment to initialize

        client.heartbeat(
                new EmptyFunction() {
                    @Override
                    public void invoke() {
                        System.out.println("java hb error");
                    }
                });

        client.submit("val y = 9");
    }
}