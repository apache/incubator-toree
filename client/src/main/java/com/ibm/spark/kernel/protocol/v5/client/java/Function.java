package com.ibm.spark.kernel.protocol.v5.client.java;

public interface Function<ParamType> {
    public void invoke(ParamType param);
}
