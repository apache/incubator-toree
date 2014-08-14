package examples;

import com.ibm.spark.SparkKernelClientBootstrap;
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
        SparkKernelClient client = new SparkKernelClient(SparkKernelClientBootstrap.createClient());

        Thread.sleep(100); // actor system takes a moment to initialize

        client.heartbeat(
                new EmptyFunction() {
                    @Override
                    public void invoke() {
                        System.out.println("java hb error");
                    }
                });

        client.execute(
            "val y = 9",
            new Function<ExecuteReply>() {
                @Override
                public void invoke(ExecuteReply param) {
                    System.out.println("java reply callback called");
                }
            },
            new Function<ExecuteResult>() {
                @Override
                public void invoke(ExecuteResult param) {
                    System.out.println("java result callback called");
                }
            },
            new Function<ShellException>() {
                @Override
                public void invoke(ShellException param) {
                    System.out.println("java error callback called");
                }
            });
    }
}