#!/bin/bash
BIN=`dirname "$0"`
BIN=`cd ${BIN}; pwd`

(cd ${BIN}/../; pwd; sbt docker)
docker run --net=host -e "IP=192.168.44.44" -e "STDIN_PORT=48000" -e "SHELL_PORT=48001" -e "IOPUB_PORT=48002" -e "CONTROL_PORT=48003" -e "HB_PORT=48004" -i com.ibm/spark-kernel:v0.1.0