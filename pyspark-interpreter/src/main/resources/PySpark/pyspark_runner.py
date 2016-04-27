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

import sys, getopt, traceback, re

from py4j.java_gateway import java_import, JavaGateway, GatewayClient
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
from pyspark.sql import SQLContext, HiveContext, SchemaRDD, Row

client = GatewayClient(port=int(sys.argv[1]))
sparkVersion = sys.argv[2]

if re.match("^1\.[456]\..*$", sparkVersion):
  gateway = JavaGateway(client, auto_convert = True)
else:
  gateway = JavaGateway(client)

java_import(gateway.jvm, "org.apache.spark.SparkEnv")
java_import(gateway.jvm, "org.apache.spark.SparkConf")
java_import(gateway.jvm, "org.apache.spark.api.java.*")
java_import(gateway.jvm, "org.apache.spark.api.python.*")
java_import(gateway.jvm, "org.apache.spark.mllib.api.python.*")

bridge = gateway.entry_point
state = bridge.state()
state.markReady()

#jsc = bridge.javaSparkContext()

if sparkVersion.startswith("1.2"):
  java_import(gateway.jvm, "org.apache.spark.sql.SQLContext")
  java_import(gateway.jvm, "org.apache.spark.sql.hive.HiveContext")
  java_import(gateway.jvm, "org.apache.spark.sql.hive.LocalHiveContext")
  java_import(gateway.jvm, "org.apache.spark.sql.hive.TestHiveContext")
elif sparkVersion.startswith("1.3"):
  java_import(gateway.jvm, "org.apache.spark.sql.*")
  java_import(gateway.jvm, "org.apache.spark.sql.hive.*")
elif re.match("^1\.[456]\..*$", sparkVersion):
  java_import(gateway.jvm, "org.apache.spark.sql.*")
  java_import(gateway.jvm, "org.apache.spark.sql.hive.*")

java_import(gateway.jvm, "scala.Tuple2")


sc = None
sqlContext = None

kernel = bridge.kernel()

class Logger(object):
  def __init__(self):
    self.out = ""

  def write(self, message):
    self.out = self.out + message

  def get(self):
    return self.out

  def reset(self):
    self.out = ""

output = Logger()
sys.stdout = output
sys.stderr = output

while True :
  try:
    code_info = state.nextCode()

    # If code is not available, try again later
    if (code_info is None):
      sleep(1)
      continue

    code_lines = code_info.code().split("\n")
    #jobGroup = req.jobGroup()
    final_code = None

    for s in code_lines:
      if s == None or len(s.strip()) == 0:
        continue

      # skip comment
      if s.strip().startswith("#"):
        continue

      if final_code:
        final_code += "\n" + s
      else:
        final_code = s

    if sc is None:
      jsc = kernel.javaSparkContext()
      if jsc is not None:
        jconf = kernel.sparkConf()
        conf = SparkConf(_jvm = gateway.jvm, _jconf = jconf)
        sc = SparkContext(jsc = jsc, gateway = gateway, conf = conf)

    if sqlContext is None:
      jsqlContext = kernel.sqlContext()
      if jsqlContext is not None and sc is not None:
        sqlContext = SQLContext(sc, sqlContext=jsqlContext)

    if final_code:
      compiled_code = compile(final_code, "<string>", "exec")
      #sc.setJobGroup(jobGroup, "Spark Kernel")
      eval(compiled_code)

    state.markSuccess(code_info.codeId(), output.get())
  except Py4JJavaError:
    excInnerError = traceback.format_exc() # format_tb() does not return the inner exception
    innerErrorStart = excInnerError.find("Py4JJavaError:")
    if innerErrorStart > -1:
       excInnerError = excInnerError[innerErrorStart:]
    state.markFailure(code_info.codeId(), excInnerError + str(sys.exc_info()))
  except:
    state.markFailure(code_info.codeId(), traceback.format_exc())

  output.reset()
