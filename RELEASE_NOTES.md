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

## 0.1.0-incubating (2016-xx-xx)

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
