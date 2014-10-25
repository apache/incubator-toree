package com.ibm.spark.client.java;

public interface Function<ParamType> {
    public void invoke(ParamType param);
}
