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

class ToreeScalaKernelTests(jupyter_kernel_test.KernelTests):
    # Required --------------------------------------

    # The name identifying an installed kernel to run the tests against
    kernel_name = "apache_toree_scala"

    # language_info.name in a kernel_info_reply should match this
    language_name = "scala"

    # Optional --------------------------------------

    # Code in the kernel's language to write "hello, world" to stdout
    code_hello_world = "println(\"hello, world\")"

    test_statements_execute_result = [
        {'code': '6*7', 'result': '42'},
        {'code': 'sc.parallelize(List(1, 2, 3, 4)).map(_*2).reduce(_+_)', 'result': '20'},
        {'code': '%showtypes on\n1', 'result': 'Int = 1'},
        {'code': '%showtypes off\n1', 'result': '1'}
    ]

    test_statements_stdout = [
        {'code': '%AddJar http://0.0.0.0:8000/TestJar.jar\nimport com.ibm.testjar.TestClass\nprintln(new TestClass().sayHello("Person"))', 'result': 'Hello, Person\n'}
    ]

    def test_scala_stdout(self):
        '''Asserts test_statements execute correctly meaning the last message is the expected result'''
        for sample in self.test_statements_stdout:
            with self.subTest(code=sample['code']):
                self.flush_channels()
                reply, output_msgs = self.execute_helper(sample['code'])

                self.assertEqual(reply['content']['status'], 'ok')

                self.assertGreaterEqual(len(output_msgs), 1)
                self.assertEqual(output_msgs[-1]['msg_type'], 'stream')
                self.assertEqual(output_msgs[-1]['content']['name'], 'stdout')
                self.assertIn(sample['result'], output_msgs[-1]['content']['text'])

    def test_scala_execute_result(self):
        '''Asserts test_statements execute correctly meaning the last message is the expected result'''
        for sample in self.test_statements_execute_result:
            with self.subTest(code=sample['code']):
                self.flush_channels()

                reply, output_msgs = self.execute_helper(sample['code'])
                self.assertEqual(reply['content']['status'], 'ok')
                #Use last message as code may be multiple lines/stream
                self.assertIn('text/plain', output_msgs[-1]['content']['data'])
                self.assertEqual(output_msgs[-1]['content']['data']['text/plain'], sample['result'])

    def execute_helper(self, code, timeout=15,
                       silent=False, store_history=True):
        '''Overrides the jupyter kernel test execute_helper'''
        self.kc.execute(code=code, silent=silent, store_history=store_history)

        reply = self.kc.get_shell_msg(timeout=timeout)

        output_msgs = []
        while True:
            msg = self.kc.iopub_channel.get_msg(timeout=0.1)
            if msg['msg_type'] == 'status':
                if msg['content']['execution_state'] == 'busy':
                    continue
                elif msg['content']['execution_state'] == 'idle':
                    break
            elif msg['msg_type'] == 'execute_input':
                self.assertEqual(msg['content']['code'], code)
                continue
            output_msgs.append(msg)

        return reply, output_msgs

class ToreePythonKernelTests(jupyter_kernel_test.KernelTests):
    # Required --------------------------------------

    # The name identifying an installed kernel to run the tests against
    kernel_name = "apache_toree_pyspark"

    # language_info.name in a kernel_info_reply should match this
    language_name = "python"

    # Optional --------------------------------------

    # Code in the kernel's language to write "hello, world" to stdout
    code_hello_world = "print(\"hello, world\")"

    # Samples of code which generate a result value (ie, some text
    # displayed as Out[n])
    code_execute_result = [
        {'code': '6*7', 'result': '42'}
    ]

if __name__ == '__main__':
    unittest.main()
