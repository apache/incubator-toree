package com.ibm.spark.client.java;

import com.ibm.spark.client.java.EmptyFunction;
import com.ibm.spark.client.java.SparkKernelClient;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.Function1;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SparkKernelClientTest {
    private SparkKernelClient sparkKernelClient;
    private com.ibm.spark.client.SparkKernelClient mockScalaClient;

    @BeforeMethod
    public void setUp() throws Exception {
        mockScalaClient = Mockito.mock(com.ibm.spark.client.SparkKernelClient.class);
        sparkKernelClient = new SparkKernelClient(mockScalaClient);
    }

    @Test
    public void heartbeat_Fail_CallsFailureCallback() throws Exception {
        // Mock the callbacks
        EmptyFunction mockFailure = Mockito.mock(EmptyFunction.class);

        sparkKernelClient.heartbeat(mockFailure);

        // Create an ArgumentCaptor to catch the AbstractFunction created in the class
        ArgumentCaptor<AbstractFunction0> failureCaptor = ArgumentCaptor.forClass(AbstractFunction0.class);
        Mockito.verify(mockScalaClient).heartbeat(failureCaptor.capture());

        // Invoke the failure, which mocks a client error
        failureCaptor.getValue().apply();

        // Verify failure was called and success was not
        Mockito.verify(mockFailure).invoke();
    }

    @Test
    public void submit_calls_scalaClientSubmit() {
        sparkKernelClient.submit("foo code");
        Mockito.verify(mockScalaClient).submit(Matchers.any(String.class));
    }

    @Test
    public void stream_calls_scalaClientStream() {
        Function1 func = Mockito.mock(AbstractFunction1.class);
        sparkKernelClient.stream("bar code", func);
        Mockito.verify(mockScalaClient).stream(Matchers.any(String.class), Matchers.any(Function1.class));
    }
}
