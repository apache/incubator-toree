package com.ibm.spark.kernel.client.java;

import com.ibm.spark.client.exception.ShellException;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteReply;
import com.ibm.spark.kernel.protocol.v5.content.ExecuteResult;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;

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
        // Mock the callbacks
        Function<ExecuteReply> mockSuccess = mock(Function.class);
        Function<ExecuteResult> mockResult  = mock(Function.class);
        Function<ShellException> mockFailure = mock(Function.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockResult, mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction1> successCaptor = ArgumentCaptor.forClass(AbstractFunction1.class);
        verify(mockScalaClient).execute(any(String.class), successCaptor.capture(),
                any(AbstractFunction1.class), any(AbstractFunction1.class));

        // Invoke the success, which mocks the client receiving ExecuteReply
        Object param = mock(Object.class);
        successCaptor.getValue().apply(param);

        // Verify success was called and others were not
        verify(mockSuccess).invoke(any(ExecuteReply.class));
        verify(mockFailure, times(0)).invoke(any(ShellException.class));
    }

    @Test
    public void execute_Failure_CallsFailureCallback() throws Exception {
        // Mock the callbacks
        Function<ExecuteReply> mockSuccess = mock(Function.class);
        Function<ExecuteResult> mockResult  = mock(Function.class);
        Function<ShellException> mockFailure = mock(Function.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockResult, mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction1> failureCaptor = ArgumentCaptor.forClass(AbstractFunction1.class);
        verify(mockScalaClient).execute(any(String.class), any(AbstractFunction1.class),
                any(AbstractFunction1.class), failureCaptor.capture());

        // Invoke the failure, which mocks a client error
        Object param = mock(Object.class);
        failureCaptor.getValue().apply(param);

        // Verify failure was called and others were not
        verify(mockSuccess, times(0)).invoke(any(ExecuteReply.class));
        verify(mockFailure).invoke(any(ShellException.class));
    }

    @Test
    public void execute_Result_CallsResultCallback() throws Exception {
        // Mock the callbacks
        Function<ExecuteReply> mockSuccess = mock(Function.class);
        Function<ExecuteResult> mockResult  = mock(Function.class);
        Function<ShellException> mockFailure = mock(Function.class);

        sparkKernelClient.execute("foo code", mockSuccess, mockResult, mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction1> resultCaptor = ArgumentCaptor.forClass(AbstractFunction1.class);
        verify(mockScalaClient).execute(any(String.class), any(AbstractFunction1.class),
                resultCaptor.capture(), any(AbstractFunction1.class));

        // Invoke the failure, which mocks a client error
        Object param = mock(Object.class);
        resultCaptor.getValue().apply(param);

        // Verify result was called and others were not
        verify(mockResult).invoke(any(ExecuteResult.class));
        verify(mockFailure, times(0)).invoke(any(ShellException.class));
    }
}