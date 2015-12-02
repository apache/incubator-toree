[![Build Status][build-badge]][build-url]
[![License][license-badge]][license-url]
[![Join the chat at https://gitter.im/ibm-et/spark-kernel][gitter-badge]][gitter-url]

Spark Kernel
============
The main goal of the Spark Kernel is to provide the foundation for interactive applications to connect to and use [Apache Spark][1].

Overview
========
The Spark Kernel provides an interface that allows clients to interact with a Spark Cluster. Clients can send libraries and snippets of code that are interpreted and ran against a preconfigured Spark context. These snippets can do a variety of things:
 1. Define and run spark jobs of all kinds
 2. Collect results from spark and push them to the client
 3. Load necessary dependencies for the running code
 4. Start and monitor a stream
 5. ...

The kernel's main supported language is `Scala`, but it is also capable of processing both `Python` and `R`. It implements the latest Jupyter message protocol (5.0), so it can easily plug into the 3.x branch of Jupyter/IPython for quick, interactive data exploration.

Try It
======
A version of the Spark Kernel is deployed as part of the [Try Jupyter!][try-jupyter] site. Select `Scala 2.10.4 (Spark 1.4.1)` under the `New` dropdown. Note that this version only supports `Scala`.

Develop
=======
[Vagrant][vagrant] is used to simplify the development experience. It is the only requirement to be able to build, package and test the Spark Kernel on your development machine. 

To build and interact with the Spark Kernel using Jupyter, run
```
make dev
```

This will start a Jupyter notebook server accessible at `http://192.168.44.44:8888`. From here you can create notebooks that use the Spark Kernel configured for local mode.

Tests can be run by doing `make test`.

Build & Package
===============
To build and package up the Spark Kernel, run
```
make dist
```

The resulting package of the kernel will be located at `./dist/spark-kernel-<VERSION>.tar.gz`. The uncompressed package is what is used is ran by Jupyter when doing `make dev`.


Version
=======
Our goal is to keep `master` up to date with the latest version of Spark. When new versions of Spark require code changes, we create a separate branch. The table below shows what is available now.

Branch                       | Spark Kernel Version | Apache Spark Version
---------------------------- | -------------------- | --------------------
[master][master]             | 0.1.5                | 1.5.1
[branch-0.1.4][branch-0.1.4] | 0.1.4                | 1.4.1
[branch-0.1.3][branch-0.1.3] | 0.1.3                | 1.3.1

Please note that for the most part, new features to Spark Kernel will only be added to the `master` branch.

Resources
=========

There is more detailed information available in our [Wiki][5] and our [Getting Started][4] guide.

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
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-url]: https://gitter.im/ibm-et/spark-kernel
[try-jupyter]: http://try.jupyter.org
[vagrant]: https://www.vagrantup.com/

[master]: https://github.com/ibm-et/spark-kernel
[branch-0.1.4]: https://github.com/ibm-et/spark-kernel/tree/branch-0.1.4
[branch-0.1.3]: https://github.com/ibm-et/spark-kernel/tree/branch-0.1.3
