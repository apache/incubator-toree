package examples;

import com.ibm.spark.SparkKernelClientBootstrap;
import com.ibm.spark.client.SparkKernelClientOptions;
import com.ibm.spark.kernel.client.java.EmptyFunction;
import com.ibm.spark.kernel.client.java.SparkKernelClient;
import scala.Function1;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/**
 * This main class demonstrates how to use the spark client in java.
 * Use this class as a playground.
 */
public class JavaSparkClientUsage {

    public static void main(String[] args) throws Exception {
        SparkKernelClient client = new SparkKernelClient(
                new SparkKernelClientBootstrap(
                        new SparkKernelClientOptions(args).toConfig()).createClient());

        Thread.sleep(100); // actor system takes a moment to initialize

        client.heartbeat(
                new EmptyFunction() {
                    @Override
                    public void invoke() {
                        System.out.println("java hb error");
                    }
                });

        client.submit("val y = 9");

        String code = "val s = new Thread(new Runnable {def run() {while(true) {Thread.sleep(1000); println(\"bar\")}}}); s.start()";
        Function1 func = new AbstractFunction1<Object, BoxedUnit>() {
            @Override
            public BoxedUnit apply(Object v1) {
                System.out.println(v1);
                return null;
            }
        };

        client.stream(code, func);
    }
}
