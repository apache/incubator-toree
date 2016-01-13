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

.PHONY: help clean clean-dist build dev test test-travis

VERSION?=0.1.5
IS_SNAPSHOT?=true
APACHE_SPARK_VERSION?=1.5.1

ifeq ($(IS_SNAPSHOT),true)
SNAPSHOT:='-SNAPSHOT'
endif

USE_VAGRANT?=
RUN_PREFIX=$(if $(USE_VAGRANT),vagrant ssh -c "cd $(VM_WORKDIR) && )
RUN_SUFFIX=$(if $(USE_VAGRANT),")

RUN=$(RUN_PREFIX)$(1)$(RUN_SUFFIX)

ENV_OPTS:=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

FULL_VERSION:=$(VERSION)$(SNAPSHOT)
ASSEMBLY_JAR:=toree-kernel-assembly-$(FULL_VERSION).jar

help:
	@echo '      clean - clean build files'
	@echo '        dev - starts ipython'
	@echo '       dist - build a packaged distribution'
	@echo '      build - builds assembly'
	@echo '       test - run all units'

build-info:
	@echo '$(ENV_OPTS) $(FULL_VERSION)'

clean-dist:
	-rm -r dist

clean: VM_WORKDIR=/src/toree-kernel
clean: clean-dist
	$(call RUN,$(ENV_OPTS) sbt clean)

kernel/target/scala-2.10/$(ASSEMBLY_JAR): VM_WORKDIR=/src/toree-kernel
kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): project/build.properties project/Build.scala project/Common.scala project/plugins.sbt
	$(call RUN,$(ENV_OPTS) sbt toree-kernel/assembly)

build: kernel/target/scala-2.10/$(ASSEMBLY_JAR)

dev: VM_WORKDIR=~
dev: dist
	$(call RUN,ipython notebook --ip=* --no-browser)

test: VM_WORKDIR=/src/toree-kernel
test:
	$(call RUN,$(ENV_OPTS) sbt compile test)

dist: COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
dist: VERSION_FILE=dist/toree-kernel/VERSION
dist: kernel/target/scala-2.10/$(ASSEMBLY_JAR) ${shell find ./etc/bin/*}
	@mkdir -p dist/toree-kernel/bin dist/toree-kernel/lib
	@cp -r etc/bin/* dist/toree-kernel/bin/.
	@cp kernel/target/scala-2.10/$(ASSEMBLY_JAR) dist/toree-kernel/lib/.
	@echo "VERSION: $(FULL_VERSION)" > $(VERSION_FILE)
	@echo "COMMIT: $(COMMIT)" >> $(VERSION_FILE)
	@cd dist; tar -cvzf toree-kernel-$(FULL_VERSION).tar.gz toree-kernel

test-travis:
	$(ENV_OPTS) sbt clean test -Dakka.test.timefactor=3
	find $(HOME)/.sbt -name "*.lock" | xargs rm
	find $(HOME)/.ivy2 -name "ivydata-*.properties" | xargs rm
