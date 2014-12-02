package com.ibm.spark.kernel.protocol.v5.client.java;

import com.ibm.spark.kernel.protocol.v5.client.execution.DeferredExecution;
import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;
import scala.runtime.BoxedUnit;

/**
 * Java adapter for {@link com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient}
 */
public class SparkKernelClient {

    protected com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient client;

    public SparkKernelClient(com.ibm.spark.kernel.protocol.v5.client.SparkKernelClient client) {
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
    public DeferredExecution execute(String code) {
        return this.client.execute(code);
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