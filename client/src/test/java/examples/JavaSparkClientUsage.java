package examples;

import com.ibm.spark.client.SparkKernelClientBootstrap;
import com.ibm.spark.client.java.EmptyFunction;
import com.ibm.spark.client.java.SparkKernelClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.Function1;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import java.io.File;
import java.net.URL;

/**
 * This main class demonstrates how to use the spark client in java.
 * Use this class as a playground.
 */
public class JavaSparkClientUsage {

    public static void main(String[] args) throws Exception {
        URL url = URL.class.getClass().getResource("/resources/test/kernel-profiles/IOPubIntegrationProfile.json");
        File f = new File(url.getFile());
        Config config = ConfigFactory.parseFile(f);

        SparkKernelClient client = new SparkKernelClient(
                new SparkKernelClientBootstrap(config).createClient());

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
