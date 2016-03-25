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

.PHONY: help clean clean-dist build dev test test-travis release pip-release bin-release dev-binder .binder-image

VERSION?=0.1.0.dev5
COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
ifeq (, $(findstring dev, $(VERSION)))
IS_SNAPSHOT?=false
else
IS_SNAPSHOT?=true
SNAPSHOT:=-SNAPSHOT
endif

APACHE_SPARK_VERSION?=1.5.1
IMAGE?=jupyter/pyspark-notebook:2988869079e6
EXAMPLE_IMAGE?=apache/toree-examples
BINDER_IMAGE?=apache/toree-binder
DOCKER_WORKDIR?=/srv/toree
DOCKER_ARGS?=
define DOCKER
docker run -it --rm \
	--workdir $(DOCKER_WORKDIR) \
	-e PYTHONPATH='/srv/toree' \
	-v `pwd`:/srv/toree $(DOCKER_ARGS)
endef

define GEN_PIP_PACKAGE_INFO
printf "__version__ = '$(VERSION)'\n" >> dist/toree/_version.py
printf "__commit__ = '$(COMMIT)'\n" >> dist/toree/_version.py
endef

USE_VAGRANT?=
RUN_PREFIX=$(if $(USE_VAGRANT),vagrant ssh -c "cd $(VM_WORKDIR) && )
RUN_SUFFIX=$(if $(USE_VAGRANT),")

RUN=$(RUN_PREFIX)$(1)$(RUN_SUFFIX)

ENV_OPTS:=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

ASSEMBLY_JAR:=toree-kernel-assembly-$(VERSION)$(SNAPSHOT).jar

help:
	@echo '      clean - clean build files'
	@echo '        dev - starts ipython'
	@echo '       dist - build a directory with contents to package'
	@echo '      build - builds assembly'
	@echo '       test - run all units'
	@echo '    release - creates packaged distribution'
	@echo '    jupyter - starts a Jupyter Notebook with Toree installed'

build-info:
	@echo '$(ENV_OPTS) $(VERSION)'

clean-dist:
	-rm -r dist

clean: VM_WORKDIR=/src/toree-kernel
clean: clean-dist
	$(call RUN,$(ENV_OPTS) sbt clean)
	rm -r `find . -name target -type d`

.example-image: EXTRA_CMD?=pip install jupyter_declarativewidgets==0.4.0; jupyter declarativewidgets install --user; jupyter declarativewidgets activate; pip install jupyter_dashboards; jupyter dashboards install --user; jupyter dashboards activate; apt-get update; apt-get install --yes curl; curl --silent --location https://deb.nodesource.com/setup_0.12 | sudo bash -; apt-get install --yes nodejs; npm install -g bower;
.example-image:
	@-docker rm -f examples_image
	@docker run -it --user root --name examples_image \
		$(IMAGE) bash -c '$(EXTRA_CMD)'
	@docker commit examples_image $(EXAMPLE_IMAGE)
	@-docker rm -f examples_image
	touch $@

.binder-image:
	@docker build --rm -t $(BINDER_IMAGE) .

dev-binder: .binder-image
	docker run --rm -it -p 8888:8888  -v `pwd`:/home/main/notebooks --workdir /home/main/notebooks $(BINDER_IMAGE) /home/main/start-notebook.sh --ip=0.0.0.0

kernel/target/scala-2.10/$(ASSEMBLY_JAR): VM_WORKDIR=/src/toree-kernel
kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
kernel/target/scala-2.10/$(ASSEMBLY_JAR): project/build.properties project/Build.scala project/Common.scala project/plugins.sbt
	$(call RUN,$(ENV_OPTS) sbt -v toree-kernel/assembly)

build: kernel/target/scala-2.10/$(ASSEMBLY_JAR)

dev: DOCKER_WORKDIR=/srv/toree/etc/examples/notebooks
dev: SUSPEND=n
dev: DEBUG_PORT=5005
dev: .example-image dist
	@$(DOCKER) \
		-e SPARK_OPTS="--driver-java-options=-agentlib:jdwp=transport=dt_socket,server=y,suspend=$(SUSPEND),address=5005" \
		-v `pwd`/etc/kernel.json:/usr/local/share/jupyter/kernels/toree/kernel.json \
		-p $(DEBUG_PORT):5005 -p 8888:8888 \
		--user=root  $(EXAMPLE_IMAGE) \
		bash -c "cp -r /srv/toree/dist/toree/* /usr/local/share/jupyter/kernels/toree/. && jupyter notebook --ip=* --no-browser"

test: VM_WORKDIR=/src/toree-kernel
test:
	$(call RUN,$(ENV_OPTS) sbt compile test)

dist: VERSION_FILE=dist/toree/VERSION
dist: kernel/target/scala-2.10/$(ASSEMBLY_JAR) ${shell find ./etc/bin/*}
	@mkdir -p dist/toree/bin dist/toree/lib
	@cp -r etc/bin/* dist/toree/bin/.
	@cp kernel/target/scala-2.10/$(ASSEMBLY_JAR) dist/toree/lib/.
	@echo "VERSION: $(VERSION)" > $(VERSION_FILE)
	@echo "COMMIT: $(COMMIT)" >> $(VERSION_FILE)

test-travis:
	$(ENV_OPTS) sbt clean test -Dakka.test.timefactor=3
	find $(HOME)/.sbt -name "*.lock" | xargs rm
	find $(HOME)/.ivy2 -name "ivydata-*.properties" | xargs rm

pip-release: DOCKER_WORKDIR=/srv/toree/dist
pip-release: dist
	@cp -rf etc/pip_install/* dist/.
	@$(GEN_PIP_PACKAGE_INFO)
	@$(DOCKER) $(IMAGE) python setup.py sdist --dist-dir=.
	@$(DOCKER) -p 8888:8888 --user=root  $(IMAGE) bash -c	'pip install toree-$(VERSION).tar.gz && jupyter toree install'

bin-release: dist
	@(cd dist; tar -cvzf toree-$(VERSION)-binary-release.tar.gz toree)

release: DOCKER_WORKDIR=/srv/toree/dist
release: PYPI_REPO?=https://pypi.python.org/pypi
release: PYPI_USER?=
release: PYPI_PASSWORD?=
release: PYPIRC=printf "[distutils]\nindex-servers =\n\tpypi\n\n[pypi]\nrepository: $(PYPI_REPO) \nusername: $(PYPI_USER)\npassword: $(PYPI_PASSWORD)" > ~/.pypirc;
release: pip-release bin-release
	@$(DOCKER) $(IMAGE) bash -c '$(PYPIRC) pip install twine && \
		python setup.py register -r $(PYPI_REPO) && \
		twine upload -r pypi toree-$(VERSION).tar.gz'

jupyter: DOCKER_WORKDIR=/srv/toree/dist
jupyter: .example-image pip-release
	@$(DOCKER) -p 8888:8888 --user=root  $(EXAMPLE_IMAGE) bash -c	'pip install toree-$(VERSION).tar.gz && jupyter toree install && cd /srv/toree/etc/examples/notebooks && jupyter notebook --ip=* --no-browser'
