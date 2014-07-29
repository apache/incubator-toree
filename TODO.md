TODO
====

The following are items that should be accomplished should we move to a 
backlog of tasks to complete.

* HistoryReply definition

    * Play's JSON framework does not support Tuples, so this would need to be
      added manually
     
    * The current definition is (String, String, String) as a List[String], 
      meaning that we do not support the second case of 
      (String, String, (String, String)) as a reply
      
    * The current definition is a List[String] field, 
      which means that unlimited messages could be fed to the construct; so, 
      defining Tuple support would allow us to check specifically for a 
      three-element array in JSON and also output a three-element array

* IPython shim layer

    * IPython notebook does not support connecting to an existing kernel (only
      the console and qtconsole support this)

    * Solution is to have a shim kernel that the IPython notebook starts up,
      which serves as a proxy to a real kernel

    * Thought is to reuse our SparkKernel code to have two sets of bindings,
      one for a frontend and another for a backend

        java -jar shimkernel.jar \
            --proxy_from {connection_file} \
            --proxy_to {kernel_file}

