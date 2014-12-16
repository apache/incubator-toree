Spark Kernel
============

A simple Scala application to connect to a Spark cluster and provide a generic,
robust API to tap into various Spark APIs. Furthermore, this project intends to
provide the ability to send both packaged jars (standard jobs) and code
snippets (with revision capability) for scenarios like IPython for dynamic
updates. Finally, the kernel is written with the future plan to allow multiple
applications to connect to a single kernel to take advantage of the same
Spark context.

Mailing list
------------

The mailing list for developers and users of the Spark Kernel can be found
here:

    spark-kernel@googlegroups.com 

The forum equivalent of the mailing list can be found here:

    https://groups.google.com/forum/#!forum/spark-kernel

Vagrant Dev Environment
-----------------------

A Vagrantfile is provided to easily setup a development environment. You will 
need to install [Virtualbox 4.3.12+](https://www.virtualbox.org/wiki/Downloads) 
and [Vagrant 1.6.2+](https://www.vagrantup.com/downloads.html). 

First, from the root of the project, bring up the vagrant box:

    vagrant up
    
Second, ssh into the vagrant box:

    vagrant ssh
    
Third, to interact with the Spark Kernel, you will need to start the IPython notebook server:

    ipython notebook --ip=0.0.0.0 --no-browser
    
You can now find the notebook frontend by going to http://192.168.44.44:8888. The source code for the project can be found in the `/src/spark-kernel` directory.

Building from Source
--------------------

To build the kernel from source, you need to have 
[sbt](http://www.scala-sbt.org/download.html) installed on your machine. Once
it is on your path, you can compile the Spark Kernel by running the following
from the root of the Spark Kernel directory:

    sbt compile

The recommended configuration options for sbt are as follows:

    -Xms1024M
    -Xmx2048M
    -Xss1M
    -XX:+CMSClassUnloadingEnabled
    -XX:MaxPermSize=1024M

Library Dependencies
--------------------

The Spark Kernel uses _ZeroMQ_ as the medium for communication. In order for
the Spark Kernel to be able to use this protocol, the ZeroMQ library needs to
be installed on the system where the kernel will be run. The _Vagrant_ and
_Docker_ environments set this up for you.

For Mac OS X, you can use [Homebrew](http://brew.sh/) to install the library:

    brew install zeromq22

For aptitude-based systems (such as Ubuntu 14.04), you can install via 
_apt-get_:

    apt-get install libzmq-dev
    
Usage Instructions
------------------

The Spark Kernel is provided as a series of jars, which can be executed on
their own or as part of the launch process of an IPython notebook.

The following command line options are available:

* --profile <file> - the file to load containing the ZeroMQ port information
* --help - displays the help menu detailing usage instructions
* --master - location of the Spark master (defaults to local[*])

Additionally, ZeroMQ configurations can be passed as command line arguments

* --ip <address>
* --stdin-port <port>
* --shell-port <port>
* --iopub-port <port>
* --control-port <port>
* --heartbeat-port <port>

Ports can also be specified as Environment variables:

* IP
* STDIN_PORT
* SHELL_PORT
* IOPUB_PORT
* CONTROL_PORT
* HB_PORT

Packing the kernel
------------------

We are utilizing [xerial/sbt-pack](https://github.com/xerial/sbt-pack) to
package and distribute the Spark Kernel.

You can run the following to package the kernel, which creates a directory in
_kernel/target/pack_ contains a Makefile that can be used to install the kernel
on the current machine:

    sbt kernel/pack
    
To install the kernel, run the following:
    
    cd kernel/target/pack
    make install
    
This will place the necessary jars in _~/local/kernel/current/lib_ and provides
a convient script to start the kernel located at 
_~/local/kernel/current/bin/sparkkernel_.

Running A Docker Container
---------------------------

The Spark Kernel can be run in a docker container using Docker 1.0.0+. There is 
a Dockerfile included in the root of the project. You will need to compile and 
pack the Spark Kernel before the docker image can be built.

    sbt compile
    sbt pack
    docker build -t spark-kernel .

After the image has been successfully created, you can run your container by 
executing the command:

    docker run -d -e IP=0.0.0.0 spark-kernel 

You must always include `-e IP=0.0.0.0` to allow the kernel to bind to the 
docker container's IP. The environment variables listed in the getting 
started section can be used in the docker run command. This allows you to 
explicitly set the ports for the kernel.

Development Instructions
------------------------

You must have *SBT 0.13.5+* installed. From the command line, you can attempt
to run the project by executing `sbt kernel/run <args>` from the root 
directory of the project. You can run all tests using `sbt test` (see
instructions below for more testing details).

For IntelliJ developers, you can attempt to create an IntelliJ project
structure using `sbt gen-idea`.

Running tests
-------------

There are four levels of test in this project:

1. Unit - tests that isolate a specific class/object/etc for its functionality

2. Integration - tests that illustrate functionality between multiple
   components

3. System - tests that demonstrate correctness across the entire system

4. Scratch - tests isolated in a local branch, used for quick sanity checks,
   not for actual inclusion into testing solution

To execute specific tests, run sbt with the following:

1. Unit - `sbt unit:test`

2. Integration - `sbt integration:test`

3. System - `sbt system:test`

4. Scratch - `sbt scratch:test`

To run all tests, use `sbt test`!

The naming convention for tests is as follows:

1. Unit - test classes end with _Spec_
   e.g. CompleteRequestSpec
    * Placed under _com.ibm.spark_

2. Integration - test classes end with _SpecForIntegration_
   e.g. InterpreterWithActorSpecForIntegration
    * Placed under _integration_

3. System - test classes end with _SpecForSystem_
   e.g. InputToAddJarSpecForSystem
    * Placed under _system_

4. Scratch
    * Placed under _scratch_

It is also possible to run tests for a specific project by using the following
syntax in sbt:

    sbt <PROJECT>/test

