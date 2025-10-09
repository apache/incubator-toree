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

[![Build Status][build-badge]][build-url]
[![License][license-badge]][license-url]
[![Join the chat at https://gitter.im/apache/toree][gitter-badge]][gitter-url]
<!--
[![Binder](http://mybinder.org/badge.svg)](http://mybinder.org/repo/apache/incubator-toree)
-->

[Apache Toree][website]
============
Apache Toree is a [Juypter Notebook](https://jupyter.org/) kernel. The main goal of Toree is to provide the foundation for
interactive applications that connect to and use [Apache Spark][1] using Scala language.

Overview
========
Toree provides an interface that allows clients to interact with a Spark Cluster. Clients can send libraries and
snippets of code that are interpreted and executed using a preconfigured Spark context.
These snippets can do a variety of things:
 1. Define and run spark jobs of all kinds
 2. Collect results from spark and push them to the client
 3. Load necessary dependencies for the running code
 4. Start and monitor a stream
 5. ...

Apache Toree supports the `Scala` programming language. It implements the latest Jupyter message protocol (5.0),
so it can easily plug into the latest releases of Jupyter/IPython (3.2.x+ and up) for quick, interactive data exploration.

<!--
Try It
======
A version of Toree is deployed as part of the [Try Jupyter!][try-jupyter] site. Select `Apache Toree - Scala` under
the `New` dropdown. Note that this version only supports `Scala`.
-->

Develop
=======
This project uses `make` as the entry point for build, test, and packaging. To perform a local build, you need to
install `sbt`, `jupyter/ipython`, and other development requirements locally on your machine.

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
make release
```

This results in 2 packages.

- `./dist/toree-<VERSION>-binary-release.tar.gz` is a simple package that contains JAR and executable
- `./dist/toree-<VERSION>.tar.gz` is a `pip` installable package that adds Toree as a Jupyter kernel.

NOTE: `make release` uses `docker`. Please refer to `docker` installation instructions for your system.

## Building Individual Components

### Main Toree Assembly
To build just the main Toree assembly jar (without spark-monitor-plugin):
```
sbt assembly
```
This creates: `target/scala-2.12/toree-assembly-<VERSION>.jar`

### Spark Monitor Plugin
To build the spark-monitor-plugin as a separate jar:
```
sbt sparkMonitorPlugin/assembly
```
This creates: `spark-monitor-plugin/target/scala-2.12/spark-monitor-plugin-<VERSION>.jar`

### Build All Components
To compile all projects including both the main assembly and spark-monitor-plugin:
```
sbt compile
```

**Note**: The spark-monitor-plugin is now built as a separate jar and is not included in the main Toree assembly.

## Using the Spark Monitor Plugin

To enable the Spark Monitor Plugin in your Toree application, you need to specify the path to the plugin JAR when starting Toree:

### Option 1: Command Line Parameter
```bash
# Start Toree with spark-monitor-plugin enabled
java -jar target/scala-2.12/toree-assembly-<VERSION>.jar --magic-url file:///path/to/spark-monitor-plugin/target/scala-2.12/spark-monitor-plugin-<VERSION>.jar [other-options]
```

### Option 2: Jupyter Kernel Installation
When installing Toree as a Jupyter kernel, you can specify the plugin:
```bash
jupyter toree install --spark_home=<YOUR_SPARK_PATH> --kernel_name=toree_with_monitor --toree_opts="--magic-url file:///path/to/spark-monitor-plugin-<VERSION>.jar"
```

### Option 3: Configuration File
You can also specify the plugin in a configuration file and use the `--profile` option:
```json
{
  "magic_urls": ["file:///path/to/spark-monitor-plugin-<VERSION>.jar"]
}
```
Then start with: `java -jar toree-assembly.jar --profile config.json`

**Important**:
- Make sure to use the absolute path to the spark-monitor-plugin JAR file and ensure the JAR is accessible from the location where Toree is running.
- The JAR file name does not contain "toree" prefix to avoid automatic loading as an internal plugin. This allows you to control when the SparkMonitorPlugin is enabled via the `--magic-url` parameter.

Run Examples
============
To play with the example notebooks, run
```
make jupyter
```

A notebook server will be launched in a `Docker` container with Toree and some other dependencies installed.
Refer to your `Docker` setup for the ip address. The notebook will be at `http://<ip>:8888/`.

Install
=======
This requires you to have a distribution of Apache Spark downloaded to the system where Apache Toree will run. The following commands will install Apache Toree.
```
pip install --upgrade toree
jupyter toree install --spark_home=<YOUR_SPARK_PATH>
```
Dev snapshots of Toree are located at https://dist.apache.org/repos/dist/dev/incubator/toree. To install using one
of those packages, you can use the following:
```
pip install <PIP_RELEASE_URL>
jupyter toree install --spark_home=<YOUR_SPARK_PATH>
```
where `PIP_RELEASE_URL` is one of the `pip` packages. For example:

```
pip install https://dist.apache.org/repos/dist/dev/incubator/toree/0.2.0/snapshots/dev1/toree-pip/toree-0.2.0.dev1.tar.gz
jupyter toree install --spark_home=<YOUR_SPARK_PATH>
```

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
[master][master]             | 3.x.x
[0.4.x][0.4.x]               | 2.x.x
[0.1.x][0.1.x]               | 1.6+

Please note that for the most part, new features will mainly be added to the `master` branch.

Resources
=========

We are currently enhancing our documentation, which is available in our [website][documentation].

[1]: https://spark.apache.org/

[website]: https://toree.apache.org
[documentation]: https://toree.apache.org/docs/current/user/quick-start/
[issues]: https://issues.apache.org/jira/browse/TOREE
[build-badge]: https://github.com/apache/incubator-toree/actions/workflows/build.yml/badge.svg
[build-url]: https://github.com/apache/incubator-toree/actions/workflows/build.yml
[license-badge]: https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat
[license-url]: LICENSE
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-url]: https://gitter.im/apache/toree
[try-jupyter]: https://try.jupyter.org
[mail-list]: mailto:dev@toree.apache.org

[master]: https://github.com/apache/incubator-toree
[0.1.x]: https://github.com/apache/incubator-toree/tree/0.1.x
[0.4.x]: https://github.com/apache/incubator-toree/tree/0.4.x
