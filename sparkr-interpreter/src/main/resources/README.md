<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

Spark Kernel adaptation of SparkR
=================================

Presently, the following APIs are made private in SparkR that are used by the
kernel to provide a form of communicate suitable for use as an interpreter:

1. SparkR only has an `init()` method that connects to the backend service for
   R _and_ creates a SparkContext instance. That I am aware, there is no other
   way to currently use SparkR. Because of this, a new method labelled
   `sparkR.connect()` is used that retrieves the existing port under the
   environment variable _EXISTING\_SPARKR\_BACKEND\_PORT_. This method is
   located in `sparkR.R` and is exported via the following:
   
        export("sparkR.connect")

2. SparkR low-level methods to communicate with the backend were marked private,
   but are used to communicate with our own bridge. If you need to use these invoke them with
   
        SparkR:::isInstanceOf
        SparkR:::callJMethod
        SparkR:::callJStatic
        SparkR:::newJObject
        SparkR:::removeJObject
        SparkR:::isRemoveMethod
        SparkR:::invokeJava

3. `org.apache.spark.api.r.RBackend` is marked as limited access to the
   package scope of `org.apache.spark.api.r`
   
       - To circumvent, use a reflective wrapping under 
         `org.apache.toree.kernel.interpreter.r.ReflectiveRBackend`
