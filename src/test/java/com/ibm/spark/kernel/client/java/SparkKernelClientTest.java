package com.ibm.spark.kernel.client.java;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.runtime.AbstractFunction0;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SparkKernelClientTest {
    private SparkKernelClient sparkKernelClient;
    private com.ibm.spark.client.SparkKernelClient mockScalaClient;

    @BeforeMethod
    public void setUp() throws Exception {
        mockScalaClient = mock(com.ibm.spark.client.SparkKernelClient.class);
        sparkKernelClient = new SparkKernelClient(mockScalaClient);
    }

    @Test
    public void heartbeat_Success_CallsSuccessCallback() throws Exception {
        // Mock the callbacks
        Runnable mockSuccess = mock(Runnable.class);
        Runnable mockFailure = mock(Runnable.class);

        sparkKernelClient.heartbeat(mockSuccess, mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> successCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).heartbeat(successCaptor.capture(), any(AbstractFunction0.class));

        // Invoke the success, which mocks the client receiving something
        successCaptor.getValue().apply();

        // Verify success was called and failure was not
        verify(mockSuccess).run();
        verify(mockFailure, times(0)).run();
    }

    @Test
    public void heartbeat_Fail_CallsFailureCallback() throws Exception {
        // Mock the callbacks
        Runnable mockSuccess = mock(Runnable.class);
        Runnable mockFailure = mock(Runnable.class);

        sparkKernelClient.heartbeat(mockSuccess, mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> failureCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).heartbeat(any(AbstractFunction0.class), failureCaptor.capture());

        // Invoke the failure, which mocks a client error
        failureCaptor.getValue().apply();

        // Verify failure was called and success was not
        verify(mockSuccess, times(0)).run();
        verify(mockFailure).run();
    }

    @Test
    public void execute_Success_CallsSuccessCallback() throws Exception {
        // Mock the callbacks
        Runnable mockSuccess = mock(Runnable.class);
        Runnable mockFailure = mock(Runnable.class);
        Runnable mockResult  = mock(Runnable.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockFailure, mockResult);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> successCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).execute(any(String.class), successCaptor.capture(),
                any(AbstractFunction0.class), any(AbstractFunction0.class));

        // Invoke the success, which mocks the client receiving ExecuteReply
        successCaptor.getValue().apply();

        // Verify success was called and others were not
        verify(mockSuccess).run();
        verify(mockFailure, times(0)).run();
    }

    @Test
    public void execute_Failure_CallsFailureCallback() throws Exception {
        // Mock the callbacks
        Runnable mockSuccess = mock(Runnable.class);
        Runnable mockFailure = mock(Runnable.class);
        Runnable mockResult  = mock(Runnable.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockFailure, mockResult);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> failureCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).execute(any(String.class), any(AbstractFunction0.class),
                failureCaptor.capture(), any(AbstractFunction0.class));

        // Invoke the failure, which mocks a client error
        failureCaptor.getValue().apply();

        // Verify failure was called and others were not
        verify(mockSuccess, times(0)).run();
        verify(mockFailure).run();
    }

    @Test
    public void execute_Result_CallsResultCallback() throws Exception {
        // Mock the callbacks
        Runnable mockSuccess = mock(Runnable.class);
        Runnable mockFailure = mock(Runnable.class);
        Runnable mockResult  = mock(Runnable.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockFailure, mockResult);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> resultCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        verify(mockScalaClient).execute(any(String.class), any(AbstractFunction0.class),
                any(AbstractFunction0.class), resultCaptor.capture());

        // Invoke the failure, which mocks a client error
        resultCaptor.getValue().apply();

        // Verify result was called and others were not
        verify(mockSuccess, times(0)).run();
        verify(mockFailure, times(0)).run();
        verify(mockResult).run();
    }
}