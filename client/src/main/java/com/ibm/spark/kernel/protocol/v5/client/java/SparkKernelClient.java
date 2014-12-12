/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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