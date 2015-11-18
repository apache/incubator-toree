#
# Copyright 2015 IBM Corp.
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

.PHONY: clean build init dev test

VERSION?=0.1.5
IS_SNAPSHOT?=true
APACHE_SPARK_VERSION?=1.5.1
APACHE_HADOOP_VERSION?=2.3.0

ENV_OPTS=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) APACHE_HADOOP_VERSION=$(APACHE_HADOOP_VERSION) VERSION=$(VERSION)

clean:
	vagrant ssh -c "cd /src/spark-kernel/ && sbt clean"
	@-rm -r dist

init:
	vagrant up

kernel/target/scala-2.10/kernel-assembly-$(VERSION).jar: ${shell find ./*/src/main/**/*}
kernel/target/scala-2.10/kernel-assembly-$(VERSION).jar: ${shell find ./*/build.sbt}
kernel/target/scala-2.10/kernel-assembly-$(VERSION).jar: project/build.properties project/Build.scala project/Common.scala project/plugins.sbt
	vagrant ssh -c "cd /src/spark-kernel/ && $(ENV_OPTS) sbt kernel/assembly"

build: kernel/target/scala-2.10/kernel-assembly-$(VERSION).jar

dev: dist
	vagrant ssh -c "cd ~ && ipython notebook --ip=* --no-browser"

test:
	vagrant ssh -c "cd /src/spark-kernel/ && $(ENV_OPTS) sbt compile test"

dist: build
	@mkdir -p dist/spark-kernel/bin dist/spark-kernel/lib
	@cp -r etc/bin/* dist/spark-kernel/bin/.
	@cp kernel/target/scala-2.10/kernel-assembly-*.jar dist/spark-kernel/lib/.