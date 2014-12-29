Spark Kernel Comm Documentation
===============================

The Comm API exposed by the Spark Kernel Client and Spark Kernel serves to
provide a clean method of communication between the Spark Kernel and its
clients.

The API provides the ability to create and send custom messages with the
focus on synchronizing data between a kernel and its clients, although that
use case is not enforced.

Access to the Comm API is made available for the client via 
`<client_instance>.comm` and for the kernel via `kernel.comm`.

Example of Registration and Communication
-----------------------------------------

The following example demonstrates the _client_ connecting to the _kernel_,
receiving a response, and then closing it's connection.

This is an example of registering an open callback on the _kernel_ side:

    // Register the callback to respond to being opened from the client
    kernel.comm.register("my target").addOpenHandler { 
        (commWriter, commId, targetName, data) =>
            commWriter.writeMsg(Map("response" -> "Hello World!"))
    }
    
This is the corresponding example of registering a message receiver on the
_client_ and initiating the Comm connection via _open_:

    val client: SparkKernelClient = /* Created elsewhere */

    // Register the callback to receive a message from the kernel, print it
    // out, and then close the connection
    client.comm.register("my target").addMsgHandler {
        (commWriter, commId, data) =>
            println(data("response"))
            commWriter.close()
    }
    
    // Initiate the Comm connection
    client.comm.open("my target")

Comm Events
-----------

The Comm API provides three types of events that can be captured:

1. Open

    - Triggered when the client/kernel receives an open request for a target
      that has been registered

2. Msg

    - Triggered when the client/kernel receives a Comm message for an open
      Comm instance
    
3. Close

    - Triggered when the client/kernel receives a close request for an open
      Comm instance
      
### Registering Callbacks ###

To register callbacks that are triggered during these events, the following
function is provided:

    register(<target name>)
    
This function, when invoked, registers the provided target on the 
client/kernel, but does not add any callbacks. To add functions to be called
during events, you can chain methods onto the register function.

#### Adding Open Callbacks ####

To add an open callback, use the `addOpenHandler(<function>)` method:

    register(<target name>).addOpenHandler(<function>)
    
The function is given the following four arguments:

- CommWriter

    - The instance of the Comm-based writer that can send messages back
    
- CommId

    - The id associated with the new Comm instance
    
- TargetName

    - The name of the Comm that is created

- Data (_Optional_)

    - The map of key/value pairs representing data associated with the new
      Comm instance
      
#### Adding Message Callbacks ####

To add a message callback, use the `addMsgHandler(<function>)` method:

    register(<target name>).addMsgHandler(<function>)
    
The function is given the following three arguments:

- CommWriter

    - The instance of the Comm-based writer that can send messages back
    
- CommId

    - The id associated with the Comm instance

- Data

    - The map of key/value pairs representing data associated with the
      received message
      
#### Adding Close Callbacks ####

To add a close callback, use the `addCloseHandler(<function>)` method:

    register(<target name>).addCloseHandler(<function>)
    
The function is given the following three arguments:

- CommWriter

    - Unused as the Comm instance associated with the writer has been closed
    
- CommId

    - The id associated with the Comm instance that was closed

- Data

    - The map of key/value pairs representing data associated with the
      received message

Comm Messaging
--------------

The Comm API exposes an _open_ method that initiates a new Comm instance on
both sides of the connection:

    `open(<target name>)`
    
This returns an instance of _CommWriter_ that can be used to send data via
the Comm protocol.

The kernel would initiate the connection via `kernel.comm.open(<target name>)`
while the client would start via `<client instance>.comm.open(<target name>)`.

As per the IPython protocol definition, the Comm instance can be opened from
either side.

### Using the Comm Writer ###

The Comm API provides an implementation of [java.io.Writer][1] that is used to
send _open_, _msg_, and _close_ Comm messages to the client or kernel (client
to kernel or vice versa).

The following methods are available with _CommWriter_ implementations:

1. `writeOpen(<target name> [, data])`

    - Sends an open request with the given target name and optional map of data
    
2. `writeMsg(<data>)`

    - Sends the map of data as a Comm message
    
3. `write(<character array>, <offset>, <length>)`

    - Sends the character array as a Comm message (in the same form as a 
      _Writer's_ write(...) method) with the key for the data as "message"
      
        - E.g. `commWriter.write(<array>, 0, <array length>)` translates to
        
            Data("message": "<array>")
    
3. `writeClose([data])`

    - Sends a close request with the optional map of data
    
4. `close()`

    - Sends a close request with no data

[1]: http://docs.oracle.com/javase/7/docs/api/java/io/Writer.html
