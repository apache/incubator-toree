package com.ibm.spark.kernel.client.java;

/**
 * Created by Chris on 8/15/14.
 */
public interface Function<ParamType> {
    public void invoke(ParamType param);
}