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

import os.path
import sys
import json
from traitlets import Unicode, Dict
from jupyter_client.kernelspecapp  import InstallKernelSpec
from jupyter_core.application import base_aliases
from traitlets.config.application import Application
from toree._version import __version__, __commit__

KERNEL_SPEC= {
    'display_name': '',
    'language_info': { 'name': 'scala' },
    'argv': [
        '',
        '--profile',
        '{connection_file}'
    ],
    'codemirror_mode': 'scala',
    'env': {
        'SPARK_OPTS': '',
        'MAX_INTERPRETER_THREADS': '16',
        'CAPTURE_STANDARD_OUT': 'true',
        'CAPTURE_STANDARD_ERR': 'true',
        'SEND_EMPTY_OUTPUT': 'false',
        'SPARK_HOME': '',
        'PYTHONPATH': '{0}/python:{0}/python/lib/py4j-0.8.2.1-src.zip'
    }
}

PYTHON_PATH = 'PYTHONPATH'
SPARK_HOME ='SPARK_HOME'
SPARK_OPTS = 'SPARK_OPTS'
DISPLAY_NAME='display_name'
ARGV='argv'
ENV = 'env'

class ToreeInstall(InstallKernelSpec):
    '''CLI for extension management.'''
    name = u'jupyter kernel toree'
    description = u'A Jupyter kernel for talking to spark'
    examples = '''
    jupyter toree install
    jupyter toree install --spark_home=/spark/home/dir
    jupyter toree install --spark_opts='--master=local[4]'
    jupyter toree install --kernel_name=toree_special
    '''

    spark_home = Unicode('/usr/local/spark', config=True,
        help='''Specify where the spark files can be found.'''
    )
    kernel_name = Unicode('Toree', config=True,
        help='Install the kernel spec with this name and is used as the display name in jupyter'
    )
    aliases = {
        'kernel_name': 'ToreeInstall.kernel_name',
        'spark_home': 'ToreeInstall.spark_home',
        'spark_opts': 'ToreeInstall.spark_opts'
    }
    spark_opts = Unicode('--master=local[2] --driver-java-options=-Xms1024M --driver-java-options=-Xmx4096M --driver-java-options=-Dlog4j.logLevel=info', config=True,
        help='''Specify command line arguments to proxy for spark config.'''
    )

    aliases.update(base_aliases)
    def parse_command_line(self, argv):
        super(InstallKernelSpec, self).parse_command_line(argv)

    def create_kernel_json(self, location):
        KERNEL_SPEC[DISPLAY_NAME] = self.kernel_name
        KERNEL_SPEC[ENV][SPARK_OPTS] = self.spark_opts
        KERNEL_SPEC[ENV][SPARK_HOME] = self.spark_home
        KERNEL_SPEC[ARGV][0] = os.path.join(location, 'bin', 'run.sh')
        KERNEL_SPEC[ENV][PYTHON_PATH] = KERNEL_SPEC[ENV][PYTHON_PATH].format(self.spark_home)
        kernel_json_file = os.path.join(location, 'kernel.json')
        with open(kernel_json_file, 'w+') as f:
            json.dump(KERNEL_SPEC, f, indent=2)

    def start(self):
        self.log.info('Installing toree kernel')
        here = os.path.abspath(os.path.join(os.path.dirname(__file__)))
        parent_dir = os.path.abspath(os.path.join(here, os.pardir))
        self.sourcedir = here
        install_dir = self.kernel_spec_manager.install_kernel_spec(self.sourcedir,
             kernel_name=self.kernel_name,
             user=self.user,
             prefix=self.prefix,
             replace=self.replace,
        )
        self.create_kernel_json(install_dir)


class ToreeApp(Application):
    version = __version__
    name = 'jupyter toree'
    description = '''Functions for managing the Toree kernel.
    This package was built with the following versions of Toree and Spark:

    \tToree Version: {}
    \tToree Build Commit: {}
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
