package com.ibm.spark.kernel.client.java;

import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

import java.util.concurrent.Callable;

/**
 * Java adapter for {@link com.ibm.spark.client.SparkKernelClient}
 */
public class SparkKernelClient {

    protected com.ibm.spark.client.SparkKernelClient client;

    public SparkKernelClient(com.ibm.spark.client.SparkKernelClient client) {
        this.client = client;
    }

    /**
     * Pings the Spark Kernel.
     * @param success callback on success
     * @param failure callback on failure
     */
    public void heartbeat(Runnable success, Runnable failure) {
        this.client.heartbeat(wrap(success), wrap(failure));
    }

    /**
     * Sends code to the Spark Kernel for execution.
     * @param code code to run
     * @param success callback on execute message send success
     * @param failure callback on execute message send failure
     * @param result callback to receive execution result
     */
    public void execute(String code, Runnable success, Runnable failure, Runnable result) {
        this.client.execute(code, wrap(success), wrap(failure), wrap(result));
    }

    private AbstractFunction0<BoxedUnit> wrap(final Runnable runnable) {
        return new AbstractFunction0<BoxedUnit>() {
            @Override
            public BoxedUnit apply() {
                runnable.run();
                return BoxedUnit.UNIT;
            }
        };
    }
}