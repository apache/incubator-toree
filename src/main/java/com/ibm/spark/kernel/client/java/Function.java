package com.ibm.spark.kernel.client.java;

public interface Function<ParamType> {
    public void invoke(ParamType param);
}
