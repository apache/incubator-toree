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

