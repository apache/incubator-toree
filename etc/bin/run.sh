#!/usr/bin/env bash

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
                                           ``
PROG_HOME="$(cd "`dirname "$0"`"/..; pwd)"

if [ -z "$SPARK_HOME" ]; then
  echo "SPARK_HOME must be set to the location of a Spark distribution!"
  exit 1
fi

echo "Starting Spark Kernel with SPARK_HOME=$SPARK_HOME"

KERNEL_ASSEMBLY=`(cd ${PROG_HOME}/lib; ls -1 toree-kernel-assembly-*.jar;)`

# disable randomized hash for string in Python 3.3+
export PYTHONHASHSEED=0
TOREE_ASSEMBLY=${PROG_HOME}/lib/${KERNEL_ASSEMBLY}
exec "$SPARK_HOME"/bin/spark-submit \
  ${SPARK_OPTS} \
  --driver-class-path ${TOREE_ASSEMBLY} \
  --class org.apache.toree.Main ${TOREE_ASSEMBLY} "$@"
