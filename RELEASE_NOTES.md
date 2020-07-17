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

# RELEASE_NOTES

## 0.4.0-incubating (2020.07)

* Properly reply errors to iopub.error
* Update dependency versions to match Spark 2.3.x
* Various small bugfixes
* Various small build updates/cleanup

## 0.3.0-incubating (2018.11)

* Fix JupyterLab support after the introduction of new cell metadata information
* Support for high-order functions
* Fix %Showtypes and %Truncate magics
* Added %ShowOutput magic to disable console output
* Added support for custom resolvers for %AddDeps magic
* Added support for predefined variables in SQL Statements
* Removed support for PySpark and Spark R in Toree (use specific kernels)

## 0.2.0-incubating (2018.08)

* Support Apache Spark 2.x codebase including Spark 2.2.2
* Enable Toree to run in Yarn cluster mode
* Create spark context lazily to avoid long startup times for the kernel
* Properly cleanup of temporary files/directories upon kernel shutdown
* %AddJAR now supports HDFS file format
* %AddDEP now defaults to default configuration
* Cell Interrupt now cancel running Spark jobs and works in background processes
* Interpreters now have the ability to send results other than text/plain


## 0.1.0-incubating (2017.03)

This is the first release of Toree since it joined Apache as an incubator project on December 2nd, 2015.

As part of moving to Apache, the original codebase from [SparkKernel](https://github.com/ibm-et/spark-kernel) has
been renamed, repackaged and improved in a variety of areas. It is also important to note that the version has been
reset back to `0.1.0` in favor or the version scheme used in the old project.

* Support for installation as a Jupyter kernel using pip
* New plugin framework for extending Toree. Currently only used for Magics.
* Support for sharing Spark context across different language interpreters.
* Improved AddDeps magic by using Coursier
* Kernel api to send HTML and Javascript content to the client
* Binder support for easy trial on Jupyter Notebook
* Demonstration of building an interactive streaming dashboard
