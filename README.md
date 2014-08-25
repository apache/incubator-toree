IBM Spark Kernel
================

A simple Scala application to connect to a Spark cluster and provide a generic,
robust API to tap into various Spark APIs. Furthermore, this project intends to
provide the ability to send both packaged jars (standard jobs) and code
snippets (with revision capability) for scenarios like IPython for dynamic
updates. Finally, the kernel is written with the future plan to allow multiple
applications to connect to a single kernel to take advantage of the same
Spark context.

Usage Instructions
------------------

The IBM Spark Kernel is provided as a stand-alone flat jar, which can be
executed on its own or as part of the launch process of an IPython notebook.

The following command line options are available:

* profile <file> - the file to load containing ZeroMQ port information

* create-context [yes|no] - whether or not to create a Spark context on startup

* verbose - indicates that more detailed logging should be provided

* help - displays the help menu detailing usage instructions

Development Instructions
------------------------

You must have *SBT 0.13.5* installed. From the command line, you can attempt to
run the project by executing `sbt run <args>` from the root directory of the
project. You can run all tests using `sbt test`. Finally, you can package a
flat jar using `sbt assembly`.

For IntelliJ developers, you can attempt to create an IntelliJ project
structure using `sbt gen-idea`. I would also recommend installing the following
plugins:

* Scala - for general Scala development support in IntelliJ

* SBT - for the ability to run sbt tasks from within IntelliJ (you can also
        replace the standard launch configurations for make/run/test with the
        sbt equivalents)

    * See [this link](https://github.com/orfjackal/idea-sbt-plugin/wiki) for
      documentation regarding setting up the SBT plugin

Running Tests
-------------

There are three levels of test in this project:

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
   e.g. InputToAddJarForSystem
    * Placed under _system_

4. Scratch
    * Placed under _scratch_

Migrating Remote Repos
----------------------
In the ignitio project run the following commands.
`git remote remove origin`
`git remote add origin git@github.rtp.raleigh.ibm.com:ignitio/sparkkernel.git`
