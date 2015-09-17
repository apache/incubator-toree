Spark Kernel
============

[![Build Status][build-badge]][build-url]
[![Coverage Status][coverage-badge]][coverage-url]
[![Scaladoc][scaladoc-badge]][scaladoc-url]
[![License][license-badge]][license-url]

Requires JDK 1.7 or higher!

The Spark Kernel has one main goal: provide the foundation for interactive applications to connect and use [Apache Spark][1].

Overview
========

<!-- Embedding HTML so we can align right our image -->
<!-- Using absolute cache path since cannot reference wiki image using normal relative url -->
<img src="https://raw.githubusercontent.com/wiki/ibm-et/spark-kernel/overview.png" alt="Spark Kernel Overview" title="Spark Kernel Overview" align="right" width=500px />

The kernel provides several key features for applications:

1. Define and run Spark Tasks

    - Executing Scala code dynamically in a similar fashion to the _Scala REPL_ and _Spark Shell_

2. Collect Results without a Datastore

    - Send execution results and streaming data back via the Spark Kernel to your applications

    - Use the [Comm API][2] - an abstraction of the IPython protocol - for more detailed data 
      communication and synchronization between your applications and the Spark Kernel

3. Host and Manage Applications Separately from Apache Spark

    - The _Spark Kernel_ serves as a proxy for requests to the Apache Spark cluster

The project intends to provide applications with the ability to send both packaged jars and code snippets. As it implements the latest IPython message protocol (5.0), the Spark Kernel can easily plug into the 3.x branch of IPython for quick, interactive data exploration. The Spark Kernel strives to be extensible, providing a [pluggable interface][3] for developers to add their own functionality.

Version
=======

Branch                       | Spark Kernel Version | Apache Spark Version
---------------------------- | -------------------- | --------------------
[master][master]             | 0.1.5                | 1.5.0
[branch-0.1.4][branch-0.1.4] | 0.1.4                | 1.4.1
[branch-0.1.3][branch-0.1.3] | 0.1.3                | 1.3.1

Please note that only the latest version of the Spark Kernel will be supported with new features!

Resources
=========

__If you are new to the Spark Kernel, please see the [Getting Started][4] section.__

__For more information, please visit the [Spark Kernel wiki][5].__

__For bug reporting and feature requests, please visit the [Spark Kernel issue list][6].__

[1]: https://spark.apache.org/
[2]: https://github.com/ibm-et/spark-kernel/wiki/Guide-to-the-Comm-API-of-the-Spark-Kernel-and-Spark-Kernel-Client
[3]: https://github.com/ibm-et/spark-kernel/wiki/Guide-to-Developing-Magics-for-the-Spark-Kernel
[4]: https://github.com/ibm-et/spark-kernel/wiki/Getting-Started-with-the-Spark-Kernel
[5]: https://github.com/ibm-et/spark-kernel/wiki
[6]: https://github.com/ibm-et/spark-kernel/issues

[build-badge]: https://travis-ci.org/ibm-et/spark-kernel.svg?branch=master
[build-url]: https://travis-ci.org/ibm-et/spark-kernel
[coverage-badge]: https://coveralls.io/repos/ibm-et/spark-kernel/badge.svg?branch=master
[coverage-url]: https://coveralls.io/r/ibm-et/spark-kernel?branch=master
[scaladoc-badge]: https://img.shields.io/badge/Scaladoc-Latest-34B6A8.svg?style=flat
[scaladoc-url]: http://ibm-et.github.io/spark-kernel/latest/api
[license-badge]: https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat
[license-url]: LICENSE

[master]: https://github.com/ibm-et/spark-kernel
[branch-0.1.4]: https://github.com/ibm-et/spark-kernel/tree/branch-0.1.4
[branch-0.1.3]: https://github.com/ibm-et/spark-kernel/tree/branch-0.1.3
