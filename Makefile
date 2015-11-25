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

.PHONY: clean build init dev test test-travis

VERSION?=0.1.5
IS_SNAPSHOT?=true
APACHE_SPARK_VERSION?=1.5.1

ENV_OPTS=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

FULL_VERSION=$(shell echo $(VERSION)`[ "$(IS_SNAPSHOT)" == "true" ] && (echo '-SNAPSHOT')` )
ASSEMBLY_JAR=$(shell echo kernel-assembly-$(FULL_VERSION).jar )

clean:
	vagrant ssh -c "cd /src/spark-kernel/ && sbt clean"
	@-rm -r dist

init:
	vagrant up

kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): project/build.properties project/Build.scala project/Common.scala project/plugins.sbt
	vagrant ssh -c "cd /src/spark-kernel/ && $(ENV_OPTS) sbt kernel/assembly"

build: kernel/target/scala-2.10/$(ASSEMBLY_JAR)

dev: dist
	vagrant ssh -c "cd ~ && ipython notebook --ip=* --no-browser"

test:
	vagrant ssh -c "cd /src/spark-kernel/ && $(ENV_OPTS) sbt compile test"

dist: COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
dist: VERSION_FILE=dist/spark-kernel/VERSION
dist: kernel/target/scala-2.10/$(ASSEMBLY_JAR)
	@mkdir -p dist/spark-kernel/bin dist/spark-kernel/lib
	@cp -r etc/bin/* dist/spark-kernel/bin/.
	@cp kernel/target/scala-2.10/$(ASSEMBLY_JAR) dist/spark-kernel/lib/.
	@echo "VERSION: $(FULL_VERSION)" > $(VERSION_FILE)
	@echo "COMMIT: $(COMMIT)" >> $(VERSION_FILE)
	@cd dist; tar -cvzf spark-kernel-$(FULL_VERSION).tar.gz spark-kernel

test-travis:
	$(ENV_OPTS) sbt clean test -Dakka.test.timefactor=3
	find $(HOME)/.sbt -name "*.lock" | xargs rm
	find $(HOME)/.ivy2 -name "ivydata-*.properties" | xargs rm