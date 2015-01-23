#
# Copyright 2014 IBM Corp.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM ubuntu:14.04
#   Setup
RUN gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys E56151BF
RUN echo deb http://repos.mesosphere.io/$(lsb_release -is | tr '[:upper:]' '[:lower:]') $(lsb_release -cs) main >> /etc/apt/sources.list.d/mesosphere.list
RUN apt-get update
RUN apt-get --no-install-recommends -y --force-yes install openjdk-7-jre mesos=0.20.1-1.0.ubuntu1404 libzmq-dev
ENV MESOS_NATIVE_LIBRARY /usr/local/lib/libmesos.so

#   Setup the binary we will run
ENTRYPOINT JVM_OPT="${JVM_OPT} -Dlog4j.logLevel=${LOG_LEVEL}" /app/bin/sparkkernel

#   Install the pack elements
ADD kernel/target/pack /app
RUN chmod +x /app/bin/sparkkernel
