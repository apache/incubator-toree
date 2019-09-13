#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the 'License'); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import os.path
import json
from os import listdir
from traitlets import Unicode, Dict, Set
from jupyter_client.kernelspecapp  import InstallKernelSpec
from jupyter_core.application import base_aliases
from traitlets.config.application import Application
from toree._version import __version__, __commit__
from jupyter_client.kernelspec import KernelSpec

INTERPRETER_LANGUAGES = {
    'Scala' : 'scala',
    'SQL' : 'sql'
}

PYTHON_PATH = 'PYTHONPATH'
SPARK_HOME ='SPARK_HOME'
TOREE_SPARK_OPTS = '__TOREE_SPARK_OPTS__'
TOREE_OPTS = '__TOREE_OPTS__'
DEFAULT_INTERPRETER = 'DEFAULT_INTERPRETER'
PYTHON_EXEC = 'PYTHON_EXEC'

class ToreeInstall(InstallKernelSpec):
    '''CLI for extension management.'''
    name = u'jupyter kernel toree'
    description = u'A Jupyter kernel for talking to spark'
    examples = '''
    jupyter toree install
    jupyter toree install --spark_home=/spark/home/dir
    jupyter toree install --spark_opts='--master=local[4]'
    jupyter toree install --kernel_name=toree_special
    jupyter toree install --toree_opts='--spark-context-initialization-mode none'
    jupyter toree install --interpreters=SQL
    jupyter toree install --python=python
    '''

    spark_home = Unicode(os.getenv(SPARK_HOME, '/usr/local/spark'), config=True,
        help='''Specify where the spark files can be found.'''
    )
    kernel_name = Unicode('Apache Toree', config=True,
        help='Install the kernel spec with this name. This is also used as the base of the display name in jupyter.'
    )
    interpreters = Unicode('Scala', config=True,
        help='A comma separated list of the interpreters to install. The names of the interpreters are case sensitive.'
    )
    toree_opts = Unicode('', config=True,
        help='''Specify command line arguments for Apache Toree.'''
    )
    spark_opts = Unicode('', config=True,
        help='''Specify command line arguments to proxy for spark config.'''
    )
    python_exec = Unicode('python', config=True,
        help='''Specify the python executable. Defaults to "python"'''
    )
    aliases = {
        'kernel_name': 'ToreeInstall.kernel_name',
        'spark_home': 'ToreeInstall.spark_home',
        'toree_opts': 'ToreeInstall.toree_opts',
        'spark_opts': 'ToreeInstall.spark_opts',
        'interpreters' : 'ToreeInstall.interpreters',
        'python_exec' : 'ToreeInstall.python_exec'
    }
    aliases.update(base_aliases)

    def parse_command_line(self, argv):
        super(InstallKernelSpec, self).parse_command_line(argv)

    def create_kernel_json(self, location, interpreter):

        kernel_spec = KernelSpec()
        interpreter_lang = INTERPRETER_LANGUAGES[interpreter]
        kernel_spec.display_name = '{} - {}'.format(self.kernel_name, interpreter)
        kernel_spec.language = interpreter_lang
        kernel_spec.argv = [os.path.join(location, 'bin', 'run.sh'), '--profile', '{connection_file}']
        kernel_spec.env = {
            DEFAULT_INTERPRETER : interpreter,
            # The SPARK_OPTS values are stored in TOREE_SPARK_OPTS to allow the two values to be merged when kernels
            # are run. This allows values to be specified during install, but also during runtime.
            TOREE_SPARK_OPTS : self.spark_opts,
            TOREE_OPTS : self.toree_opts,
            SPARK_HOME : self.spark_home,
        }

        kernel_json_file = os.path.join(location, 'kernel.json')
        self.log.debug('Creating kernel json file for {}'.format(interpreter))
        with open(kernel_json_file, 'w+') as f:
            json.dump(kernel_spec.to_dict(), f, indent=2)

    def start(self):
        self.log.info('Installing Apache Toree version {}'.format(__version__))

        self.sourcedir = os.path.abspath(os.path.join(os.path.dirname(__file__)))

        disclaimer_file = open(os.path.join(self.sourcedir, 'DISCLAIMER'))
        self.log.info('\n{}'.format(disclaimer_file.read()))
        for interpreter in self.interpreters.split(','):
            if interpreter in INTERPRETER_LANGUAGES:
                self.log.info('Creating kernel {}'.format(interpreter))
                install_dir = self.kernel_spec_manager.install_kernel_spec(self.sourcedir,
                     kernel_name='{}_{}'.format(self.kernel_name, interpreter.lower()).replace(' ', '_'),
                     user=self.user,
                     prefix=self.prefix,
                     replace=self.replace
                )
                self.create_kernel_json(install_dir, interpreter)
            else:
                self.log.error('Unknown interpreter {0}. Skipping installation of {0} interpreter'.format(interpreter))

class ToreeApp(Application):
    version = __version__
    name = 'jupyter toree'
    description = '''Functions for managing the Apache Toree kernel.
    This package was built with the following versions of Apache Toree and Spark:

    \tApache Toree Version: {}
    \tApache Toree Build Commit: {}
    '''.format(__version__, __commit__)
    examples = '''
    jupyter toree install - Installs the kernel as a Jupyter Kernel.
    '''
    subcommands = Dict({
        'install': (ToreeInstall, ToreeInstall.description.splitlines()[0]),
    })

    aliases = {}
    flags = {}

    def start(self):
        if self.subapp is None:
            print('No subcommand specified. Must specify one of: %s'% list(self.subcommands))
            print()
            self.print_description()
            self.print_subcommands()
            self.exit(1)
        else:
            return self.subapp.start()

def main():
    ToreeApp.launch_instance()
