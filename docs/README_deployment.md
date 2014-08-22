Guide to installing the kernel on IPython Notebook
==================================================

Packaging the kernel
--------------------

To make the kernel more easably deployable, we decided to use `sbt pack` to
provide a quick deployment package.

To install the kernel using the _pack_ plugin for _sbt_, execute the following:

1. Move into the root of the Spark Kernel directory

2. Execute `sbt pack` - generates a _pack_ directory inside `target/`
    * The _pack_ directory contains `README`, `Makefile`, `bin/`, and `lib/`

3. Move into `target/pack/`

4. Execute `make install` - generates a local bin directory with a script
   representing the Spark Kernel (currently labelled as `sparkkernel`)
    * This path is usually something like `$HOME/local/bin/sparkkernel`

Adding the configuration for a kernel to IPython
------------------------------------------------

To allow the IPython notebook to be aware of the Spark Kernel, you need to
provide a specific configuration file. This process has changed from IPython
2.x to the developmental 3.x branch that we are using.

So, first, make sure that you have IPython 3.x installed on your machine.

From there, you need to create a new subdirectory within `.ipython/kernels/`
to contain your configuration. Standard name for the Spark Kernel would be
_spark_.

Now, use your favorite editor to create a _kernel.json_ file within the
subdirectory that has similar structure to the following:

```
{
    "display_name": "Spark 1.0.1 (Scala 2.10.4)",
    "language": "scala",
    "argv": [
        "/Users/Senkwich/local/bin/sparkkernel",
        "--profile",
        "{connection_file}"
    ],
    "codemirror_mode": "scala"
}
```

Replace `/Users/Senkwich/` with the path to your home directory.

The _display name_ property is merely used in the dropdown menu of the
notebook interface.

The _argv_ property is the most significant part of the configuration as it
tells IPython what process to start as a kernel and how to provide it with the
port information IPython specifies for communication.

The _connection file_ is replaced by IPython with the path to the JSON file
containing port and signature information. That structure looks like the
following:

```
{
    "stdin_port": 48691,
    "ip": "127.0.0.1",
    "control_port": 44808,
    "hb_port": 49691,
    "signature_scheme": "hmac-sha256",
    "key": "",
    "shell_port": 40544,
    "transport": "tcp",
    "iopub_port": 43462
}
```

[CodeMirror](http://codemirror.net/) is used by IPython for cell editing and
syntax highlighting. It provides quite a few capabilities such as running cells
in _vim_. The field is optional in the above and can be set to a series of keys
and values, although a single value also works.

Ensure that your kernel is available
------------------------------------

You can test that your kernel is recognized by running the following:

1. `ipython kernelspec list` - shows a list of kernels, _spark_ should be there

2. `ipython console --kernel spark` - should launch the Spark Kernel and
    provide a console-like input

3. `ipython notebook` - open a notebook from the user interface, there should
    now be a dropdown menu (set to Python 2) in the top-right of the page that
    has an option for the Spark Kernel

