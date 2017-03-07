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
PROG_HOME="$(cd "`dirname "$0"`"/..; pwd)"

if [ -z "$SPARK_HOME" ]; then
  echo "SPARK_HOME must be set to the location of a Spark distribution!"
  exit 1
fi

echo "Starting Spark Kernel with SPARK_HOME=$SPARK_HOME"

KERNEL_ASSEMBLY=`(cd ${PROG_HOME}/lib; ls -1 toree-assembly-*.jar;)`

# disable randomized hash for string in Python 3.3+
export PYTHONHASHSEED=0
TOREE_ASSEMBLY=${PROG_HOME}/lib/${KERNEL_ASSEMBLY}
# The SPARK_OPTS values during installation are stored in __TOREE_SPARK_OPTS__. This allows values to be specified during
# install, but also during runtime. The runtime options take precedence over the install options.
if [ "${SPARK_OPTS}" = "" ]
then
   SPARK_OPTS=${__TOREE_SPARK_OPTS__}
fi

if [ "${TOREE_OPTS}" = "" ]
then
   TOREE_OPTS=${__TOREE_OPTS__}
fi

eval exec \
     "${SPARK_HOME}/bin/spark-submit" \
     --name "'Apache Toree'" \
     "${SPARK_OPTS}" \
     --class org.apache.toree.Main \
     "${TOREE_ASSEMBLY}" \
     "${TOREE_OPTS}" \
     "$@"
