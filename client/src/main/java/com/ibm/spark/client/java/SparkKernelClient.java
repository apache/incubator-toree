package com.ibm.spark.client.java;

import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

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
     * @param failure callback on failure
     */
    public void heartbeat(EmptyFunction failure) {
        this.client.heartbeat(wrap(failure));
    }

    /**
     * Sends code to the Spark Kernel for execution.
     * @param code code to run
     */
    public Future<Object> submit(String code) {
        return this.client.submit(code);
    }

    /**
     * Sends streaming code to the Spark Kernel for execution.
     * @param code code to run
     * @param func callback that gets called when the kernel prints a result.
     */
    public void stream(String code, Function1<Object, BoxedUnit> func) {
        this.client.stream(code, func);
    }

    private AbstractFunction0<BoxedUnit> wrap(final EmptyFunction func) {
        return new AbstractFunction0<BoxedUnit>() {
            @Override
            public BoxedUnit apply() {
                func.invoke();
                return BoxedUnit.UNIT;
            }
        };
    }
}