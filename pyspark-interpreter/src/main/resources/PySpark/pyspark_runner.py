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

import sys, getopt, traceback, re, ast

print("PYTHON::: Starting imports")
from py4j.java_gateway import java_import, JavaGateway, GatewayClient
print("PYTHON::: Py4J imported")
from py4j.protocol import Py4JJavaError
from pyspark.conf import SparkConf
from pyspark.context import SparkContext
from pyspark.rdd import RDD
from pyspark.files import SparkFiles
from pyspark.storagelevel import StorageLevel
from pyspark.accumulators import Accumulator, AccumulatorParam
from pyspark.broadcast import Broadcast
from pyspark.serializers import MarshalSerializer, PickleSerializer

from time import sleep

# for back compatibility
from pyspark.sql import SparkSession, DataFrame, Row

client = GatewayClient(port=int(sys.argv[1]))
sparkVersion = sys.argv[2]

print("PYTHON:: Starting gateway")
if re.match("^1\.[456]\..*$", sparkVersion) or re.match("^2\..*$", sparkVersion):
    gateway = JavaGateway(client, auto_convert=True)
else:
    gateway = JavaGateway(client)
print("PYTHON:: Gateway started")

java_import(gateway.jvm, "org.apache.spark.SparkEnv")
java_import(gateway.jvm, "org.apache.spark.SparkConf")
java_import(gateway.jvm, "org.apache.spark.api.java.*")
java_import(gateway.jvm, "org.apache.spark.api.python.*")
java_import(gateway.jvm, "org.apache.spark.mllib.api.python.*")

bridge = gateway.entry_point
state = bridge.state()
state.markReady()

if sparkVersion.startswith("1.2"):
    java_import(gateway.jvm, "org.apache.spark.sql.SparkSession")
    java_import(gateway.jvm, "org.apache.spark.sql.hive.HiveContext")
    java_import(gateway.jvm, "org.apache.spark.sql.hive.LocalHiveContext")
    java_import(gateway.jvm, "org.apache.spark.sql.hive.TestHiveContext")
elif sparkVersion.startswith("1.3"):
    java_import(gateway.jvm, "org.apache.spark.sql.*")
    java_import(gateway.jvm, "org.apache.spark.sql.hive.*")
elif re.match("^1\.[456]\..*$", sparkVersion):
    java_import(gateway.jvm, "org.apache.spark.sql.*")
    java_import(gateway.jvm, "org.apache.spark.sql.hive.*")
elif re.match("^2\..*$", sparkVersion):
    java_import(gateway.jvm, "org.apache.spark.sql.*")

java_import(gateway.jvm, "scala.Tuple2")

conf = None
sc = None
spark = None
code_info = None


class Logger(object):
    def __init__(self):
        self.out = ""

    def write(self, message):
        state.sendOutput(code_info.codeId(), message)
        self.out = self.out + message

    def get(self):
        return self.out

    def reset(self):
        self.out = ""

output = Logger()
sys.stdout = output
sys.stderr = output


class Kernel(object):
    def __init__(self, jkernel):
        self._jvm_kernel = jkernel

    def __getattr__(self, name):
        return self._jvm_kernel.__getattribute__(name)

    def __dir__(self):
        parent = super().__dir__()
        return parent + [x for x in self._jvm_kernel.__dir__() if x not in parent]

    def createSparkContext(self, config):
        jconf = gateway.jvm.org.apache.spark.SparkConf(False)
        for key,value in config.getAll():
            jconf.set(key, value)
        self._jvm_kernel.createSparkContext(jconf)
        self.refreshContext()

    def refreshContext(self):
        global conf, sc, spark

        # This is magic. Please look away. I was never here (prevents multiple gateways being instantiated)
        with SparkContext._lock:
            if not SparkContext._gateway:
                SparkContext._gateway = gateway
                SparkContext._jvm = gateway.jvm

        if sc is None:
            jsc = self._jvm_kernel.javaSparkContext()
            if jsc is not None:
                jconf = self._jvm_kernel.sparkConf()
                conf = SparkConf(_jvm=gateway.jvm, _jconf=jconf)
                sc = SparkContext(jsc=jsc, gateway=gateway, conf=conf)

        if spark is None:
            jspark = self._jvm_kernel.sparkSession()
            if jspark is not None and sc is not None:
                spark = SparkSession(sc, jsparkSession=jspark)

kernel = Kernel(bridge.kernel())

while True:
    try:
        code_info = state.nextCode()

        # If code is not available, try again later
        if code_info is None:
            sleep(1)
            continue

        code_lines = code_info.code().split("\n")
        final_code = None

        for s in code_lines:
            if s is None or len(s.strip()) == 0:
                continue

            # skip comment
            if s.strip().startswith("#"):
                continue

            if final_code:
                final_code += "\n" + s
            else:
                final_code = s

        # Ensure the appropriate variables are set in the module namespace
        kernel.refreshContext()

        if final_code:
            '''Parse the final_code to an AST parse tree.  If the last node is an expression (where an expression
            can be a print function or an operation like 1+1) turn it into an assignment where temp_val = last expression.
            The modified parse tree will get executed.  If the variable temp_val introduced is not none then we have the
            result of the last expression and should return it as an execute result.  The sys.stdout sendOutput logic
            gets triggered on each logger message to support long running code blocks instead of bulk'''
            ast_parsed = ast.parse(final_code)
            the_last_expression_to_assign_temp_value = None
            if isinstance(ast_parsed.body[-1], ast.Expr):
                new_node = (ast.Assign(targets=[ast.Name(id='the_last_expression_to_assign_temp_value', ctx=ast.Store())], value=ast_parsed.body[-1].value))
                ast_parsed.body[-1] = ast.fix_missing_locations(new_node)
            compiled_code = compile(ast_parsed, "<string>", "exec")
            eval(compiled_code)
            if the_last_expression_to_assign_temp_value is not None:
                state.markSuccess(code_info.codeId(), str(the_last_expression_to_assign_temp_value))
            else:
                state.markSuccess(code_info.codeId(), "")
            del the_last_expression_to_assign_temp_value

    except Py4JJavaError:
        excInnerError = traceback.format_exc() # format_tb() does not return the inner exception
        innerErrorStart = excInnerError.find("Py4JJavaError:")
        if innerErrorStart > -1:
            excInnerError = excInnerError[innerErrorStart:]
        state.markFailure(code_info.codeId(), excInnerError + str(sys.exc_info()))
    except:
        state.markFailure(code_info.codeId(), traceback.format_exc())

    output.reset()
