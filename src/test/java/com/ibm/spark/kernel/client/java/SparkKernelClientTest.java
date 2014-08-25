package com.ibm.spark.kernel.client.java;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SparkKernelClientTest {
    private SparkKernelClient sparkKernelClient;
    private com.ibm.spark.client.SparkKernelClient mockScalaClient;

    @BeforeMethod
    public void setUp() throws Exception {
        mockScalaClient = mock(com.ibm.spark.client.SparkKernelClient.class);
        sparkKernelClient = new SparkKernelClient(mockScalaClient);
    }

    @Test
    public void heartbeat_Fail_CallsFailureCallback() throws Exception {
        // Mock the callbacks
        EmptyFunction mockFailure = mock(EmptyFunction.class);

        sparkKernelClient.heartbeat(mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> failureCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).heartbeat(failureCaptor.capture());

        // Invoke the failure, which mocks a client error
        failureCaptor.getValue().apply();

        // Verify failure was called and success was not
        verify(mockFailure).invoke();
    }

    @Test
    public void execute_Success_CallsSuccessCallback() throws Exception {
        Future<Object> future = sparkKernelClient.submit("foo code");

        verify(mockScalaClient).submit(any(String.class));
    }
}