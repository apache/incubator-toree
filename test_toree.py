#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
#

import unittest
import jupyter_kernel_test

class ToreeKernelTests(jupyter_kernel_test.KernelTests):
    # Required --------------------------------------

    # The name identifying an installed kernel to run the tests against
    kernel_name = "apache_toree_scala"

    # language_info.name in a kernel_info_reply should match this
    language_name = "scala"

    # Optional --------------------------------------

    # the normal file extension (including the leading dot) for this language
    # checked against language_info.file_extension in kernel_info_reply
    file_extension = ".scala"

    # Code in the kernel's language to write "hello, world" to stdout
    code_hello_world = "println(\"hello, world\")"

    # Samples of code which generate a result value (ie, some text
    # displayed as Out[n])
    code_execute_result = [
        {'code': '6*7', 'result': '42'},
        {'code': 'sc.parallelize(List(1, 2, 3, 4)).map(_*2).reduce(_+_)', 'result': '20'},
        {'code': '%showtypes on\n1', 'result': 'Int = 1'},
        {'code': '%showtypes off\n1', 'result': '1'}
    ]

if __name__ == '__main__':
    unittest.main()