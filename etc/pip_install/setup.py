#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import sys
from setuptools import setup

here = os.path.abspath(os.path.dirname(__file__))

version_ns = {}
with open(os.path.join(here, 'toree', '_version.py')) as f:
    exec(f.read(), {}, version_ns)

# The incubation disclaimer is read from the DISCLAIMER file shipped alongside
# this package rather than repeated here, so the PyPI metadata cannot drift from
# the copy the ASF requires the distribution to carry. The file is indented to
# match the surrounding literal. A missing DISCLAIMER must never fail an
# install, so fall back to the summary line on its own.
long_description = '''
    This package will install Apache Toree as a Jupyter kernel.
'''
try:
    with open(os.path.join(here, 'DISCLAIMER')) as f:
        disclaimer = f.read().strip()
    long_description += '\n' + '\n'.join(
        '    ' + line if line.strip() else '' for line in disclaimer.splitlines()
    ) + '\n    '
except OSError:
    pass

setup_args = dict(
    name='toree',
    author='Apache Toree Development Team',
    author_email='dev@toree.apache.org',
    description='A Jupyter kernel for enabling remote applications to interaction with Apache Spark.',
    long_description=long_description,
    url='https://toree.apache.org/',
    version=version_ns['__version__'],
    license='Apache License, Version 2.0',
    platforms=[],
    packages=['toree'],
    include_package_data=True,
    install_requires=[
        'jupyter_core>=4.0',
        'jupyter_client>=4.0',
        'traitlets>=4.0'
    ],
    data_files=[],
    classifiers=[
        'Intended Audience :: Developers',
        'Intended Audience :: System Administrators',
        'Intended Audience :: Science/Research',
        'License :: OSI Approved :: Apache Software License',
        'Programming Language :: Python',
        'Programming Language :: SQL'
    ]
)

if 'setuptools' in sys.modules:
    # setupstools turns entrypoint scripts into executables on windows
    setup_args['entry_points'] = {
        'console_scripts': [
            'jupyter-toree = toree.toreeapp:main'
        ]
    }
    # Don't bother installing the .py scripts if if we're using entrypoints
    setup_args.pop('scripts', None)

if __name__ == '__main__':
    setup(**setup_args)
