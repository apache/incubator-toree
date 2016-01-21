[![Build Status][build-badge]][build-url]
[![License][license-badge]][license-url]
[![Join the chat at https://gitter.im/ibm-et/spark-kernel][gitter-badge]][gitter-url]

Apache Toree
============
The main goal of the Toree is to provide the foundation for interactive applications to connect to and use [Apache Spark][1].

Overview
========
Toree provides an interface that allows clients to interact with a Spark Cluster. Clients can send libraries and snippets of code that are interpreted and ran against a preconfigured Spark context. These snippets can do a variety of things:
 1. Define and run spark jobs of all kinds
 2. Collect results from spark and push them to the client
 3. Load necessary dependencies for the running code
 4. Start and monitor a stream
 5. ...

The main supported language is `Scala`, but it is also capable of processing both `Python` and `R`. It implements the latest Jupyter message protocol (5.0), so it can easily plug into the latest releases of Jupyter/IPython (3.2.x+ and 4.x+) for quick, interactive data exploration.

Try It
======
A version of Toree is deployed as part of the [Try Jupyter!][try-jupyter] site. Select `Scala 2.10.4 (Spark 1.4.1)` under the `New` dropdown. Note that this version only supports `Scala`.

Develop
=======
This project uses `make` as the entry point for build, test, and packaging. It supports 2 modes, local and vagrant. The default is local and all command (i.e. sbt) will be ran locally on your machine. This means that you need to
install `sbt`, `jupyter/ipython`, and other develoment requirements locally on your machine. The 2nd mode uses [Vagrant][vagrant] to simplify the development experience. In vagrant mode, all commands are sent to the vagrant box 
that has all necessary dependencies pre-installed. To run in vagrant mode, run `export USE_VAGRANT=true`.  

To build and interact with Toree using Jupyter, run
```
make dev
```

This will start a Jupyter notebook server. Depending on your mode, it will be accessible at `http://localhost:8888` or `http://192.168.44.44:8888`. From here you can create notebooks that use Toree configured for Spark local mode.

Tests can be run by doing `make test`.

>> NOTE: Do not use `sbt` directly.

Build & Package
===============
To build and package up Toree, run
```
make dist
```

The resulting package of the kernel will be located at `./dist//toree-kernel-<VERSION>.tar.gz`. The uncompressed package is what is used is ran by Jupyter when doing `make dev`.

Reporting Issues
================
Refer to and open issue [here][issues]

Communication
=============
You can reach us through [gitter][gitter-url] or our [mailing list][mail-list]

Version
=======
We are working on publishing binary releases of Toree soon. As part of our move into Apache Incubator, Toree will start a new version sequence starting at `0.1`. 

Our goal is to keep `master` up to date with the latest version of Spark. When new versions of Spark require specific code changes to Toree, we will branch out older Spark version support. 

As it stands, we maintain several branches for legacy versions of Spark. The table below shows what is available now.

Branch                       | Apache Spark Version
---------------------------- | --------------------
[master][master]             | 1.5.1+
[branch-0.1.4][branch-0.1.4] | 1.4.1
[branch-0.1.3][branch-0.1.3] | 1.3.1

Please note that for the most part, new features will mainly be added to the `master` branch.

Resources
=========

We are working on porting our documentation into Apache. For the time being, you can refer to this [Wiki][5] and our [Getting Started][4] guide. You may also visit our [website][website].

[1]: https://spark.apache.org/
[2]: https://github.com/ibm-et/spark-kernel/wiki/Guide-to-the-Comm-API-of-the-Spark-Kernel-and-Spark-Kernel-Client
[3]: https://github.com/ibm-et/spark-kernel/wiki/Guide-to-Developing-Magics-for-the-Spark-Kernel
[4]: https://github.com/ibm-et/spark-kernel/wiki/Getting-Started-with-the-Spark-Kernel
[5]: https://github.com/ibm-et/spark-kernel/wiki

[website]: http://toree.apache.org
[issues]: https://issues.apache.org/jira/browse/TOREE
[build-badge]: https://travis-ci.org/ibm-et/spark-kernel.svg?branch=master
[build-url]: https://travis-ci.org/ibm-et/spark-kernel
[license-badge]: https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat
[license-url]: LICENSE
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-url]: https://gitter.im/ibm-et/spark-kernel
[try-jupyter]: http://try.jupyter.org
[vagrant]: https://www.vagrantup.com/
[mail-list]: mailto:dev@toree.incubator.apache.org

[master]: https://github.com/apache/incubator-toree
[branch-0.1.4]: https://github.com/apache/incubator-toree/tree/branch-0.1.4
[branch-0.1.3]: https://github.com/apache/incubator-toree/tree/branch-0.1.3
