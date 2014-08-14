package com.ibm.spark.kernel.client.java;

import com.ibm.spark.client.exception.ShellException;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;
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
     * @param failure callback on failure
     */
    public void heartbeat(EmptyFunction failure) {
        this.client.heartbeat(wrap(failure));
    }

    /**
     * Sends code to the Spark Kernel for execution.
     * @param code code to run
     * @param success callback on execute message send success
     * @param result callback to receive execution result
     * @param failure callback on execute message send failure
     */
    public void execute(String code,  Function<ExecuteReply> success,  Function<ExecuteResult> result, Function<ShellException> failure) {
        this.client.execute(code, wrap(success), wrap(result), wrap(failure));
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

    private <T> AbstractFunction1<T, BoxedUnit> wrap(final Function<T> func) {
        return new AbstractFunction1<T, BoxedUnit>() {

            @Override
            public BoxedUnit apply(T v1) {
                func.invoke(v1);
                return BoxedUnit.UNIT;
            }
        };
    }
}