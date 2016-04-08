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

.PHONY: help clean clean-dist build dev test test-travis release pip-release bin-release dev-binder .binder-image audit

VERSION?=0.1.0.dev6-incubating
COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
ifeq (, $(findstring dev, $(VERSION)))
IS_SNAPSHOT?=false
else
IS_SNAPSHOT?=true
SNAPSHOT:=-SNAPSHOT
endif

APACHE_SPARK_VERSION?=1.6.1
IMAGE?=jupyter/pyspark-notebook:8dfd60b729bf
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
printf "__version__ = '$(VERSION)'\n" >> dist/toree-pip/toree/_version.py
printf "__commit__ = '$(COMMIT)'\n" >> dist/toree-pip/toree/_version.py
endef

USE_VAGRANT?=
RUN_PREFIX=$(if $(USE_VAGRANT),vagrant ssh -c "cd $(VM_WORKDIR) && )
RUN_SUFFIX=$(if $(USE_VAGRANT),")

RUN=$(RUN_PREFIX)$(1)$(RUN_SUFFIX)

ENV_OPTS:=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

ASSEMBLY_JAR:=toree-assembly-$(VERSION)$(SNAPSHOT).jar

help:
	@echo '      audit - run audit tools against the source code'
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

.example-image: EXTRA_CMD?=printf "deb http://cran.rstudio.com/bin/linux/debian jessie-cran3/" >> /etc/apt/sources.list; apt-key adv --keyserver keys.gnupg.net --recv-key 381BA480; apt-get update; pip install jupyter_declarativewidgets==0.4.4; jupyter declarativewidgets install --user; jupyter declarativewidgets activate; pip install jupyter_dashboards; jupyter dashboards install --user; jupyter dashboards activate; apt-get update; apt-get install --yes curl; curl --silent --location https://deb.nodesource.com/setup_0.12 | sudo bash -; apt-get install --yes nodejs r-base r-base-dev; npm install -g bower;
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
	@docker run --rm -it -p 8888:8888  \
		-v `pwd`:/home/main/notebooks \
		--workdir /home/main/notebooks $(BINDER_IMAGE) \
		/home/main/start-notebook.sh --ip=0.0.0.0

target/scala-2.10/$(ASSEMBLY_JAR): VM_WORKDIR=/src/toree-kernel
target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
target/scala-2.10/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
target/scala-2.10/$(ASSEMBLY_JAR): dist/toree-legal project/build.properties project/Build.scala project/Common.scala project/plugins.sbt
	$(call RUN,$(ENV_OPTS) sbt toree/assembly)

build: target/scala-2.10/$(ASSEMBLY_JAR)

dev: DOCKER_WORKDIR=/srv/toree/etc/examples/notebooks
dev: SUSPEND=n
dev: DEBUG_PORT=5005
dev: .example-image dist
	@$(DOCKER) \
		-e SPARK_OPTS="--master=local[4] --driver-java-options=-agentlib:jdwp=transport=dt_socket,server=y,suspend=$(SUSPEND),address=5005" \
		-v `pwd`/etc/kernel.json:/usr/local/share/jupyter/kernels/toree/kernel.json \
		-p $(DEBUG_PORT):5005 -p 8888:8888 \
		--user=root  $(EXAMPLE_IMAGE) \
		bash -c "cp -r /srv/toree/dist/toree/* /usr/local/share/jupyter/kernels/toree/. \
			&& jupyter notebook --ip=* --no-browser"

test: VM_WORKDIR=/src/toree-kernel
test:
	$(call RUN,$(ENV_OPTS) sbt compile test)

sbt-%:
	$(call RUN,$(ENV_OPTS) sbt $(subst sbt-,,$@) )

dist/toree/lib: target/scala-2.10/$(ASSEMBLY_JAR)
	@mkdir -p dist/toree/lib
	@cp target/scala-2.10/$(ASSEMBLY_JAR) dist/toree/lib/.

dist/toree/bin: ${shell find ./etc/bin/*}
	@mkdir -p dist/toree/bin
	@cp -r etc/bin/* dist/toree/bin/.

dist/toree/VERSION:
	@mkdir -p dist/toree
	@echo "VERSION: $(VERSION)" > dist/toree/VERSION
	@echo "COMMIT: $(COMMIT)" >> dist/toree/VERSION

dist/toree-legal/LICENSE: LICENSE etc/legal/LICENSE_extras
	@mkdir -p dist/toree-legal
	@cat LICENSE > dist/toree-legal/LICENSE
	@echo '\n' >> dist/toree-legal/LICENSE
	@cat etc/legal/LICENSE_extras >> dist/toree-legal/LICENSE
	@cp etc/legal/COPYING dist/toree-legal/COPYING
	@cp etc/legal/COPYING.LESSER dist/toree-legal/COPYING.LESSER

dist/toree-legal/NOTICE: NOTICE etc/legal/NOTICE_extras
	@mkdir -p dist/toree-legal
	@cat NOTICE > dist/toree-legal/NOTICE
	@echo '\n' >> dist/toree-legal/NOTICE
	@cat etc/legal/NOTICE_extras >> dist/toree-legal/NOTICE

dist/toree-legal/DISCLAIMER:
	@mkdir -p dist/toree-legal
	@cp DISCLAIMER dist/toree-legal/DISCLAIMER

dist/toree-legal: dist/toree-legal/LICENSE dist/toree-legal/NOTICE dist/toree-legal/DISCLAIMER

dist/toree: dist/toree-legal dist/toree/lib dist/toree/bin
	@cp dist/toree-legal/* dist/toree

dist: dist/toree

test-travis:
	$(ENV_OPTS) sbt clean test -Dakka.test.timefactor=3
	find $(HOME)/.sbt -name "*.lock" | xargs rm
	find $(HOME)/.ivy2 -name "ivydata-*.properties" | xargs rm

dist/toree-pip/toree-$(VERSION).tar.gz: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
dist/toree-pip/toree-$(VERSION).tar.gz: dist/toree
	@mkdir -p dist/toree-pip
	@cp -r dist/toree dist/toree-pip
	@cp dist/toree/LICENSE dist/toree-pip/LICENSE
	@cp dist/toree/NOTICE dist/toree-pip/NOTICE
	@cp dist/toree/DISCLAIMER dist/toree-pip/DISCLAIMER
	@cp dist/toree/COPYING dist/toree-pip/COPYING
	@cp dist/toree/COPYING.LESSER dist/toree-pip/COPYING.LESSER
	@cp -rf etc/pip_install/* dist/toree-pip/.
	@$(GEN_PIP_PACKAGE_INFO)
	@$(DOCKER) $(IMAGE) python setup.py sdist --dist-dir=.
	@$(DOCKER) -p 8888:8888 --user=root  $(IMAGE) bash -c	'pip install toree-$(VERSION).tar.gz && jupyter toree install'

pip-release: dist/toree-pip/toree-$(VERSION).tar.gz

dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz: dist/toree
	@mkdir -p dist/toree-bin
	@(cd dist; tar -cvzf toree-bin/toree-$(VERSION)-binary-release.tar.gz toree)

bin-release: dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz

dist/toree-src/toree-$(VERSION)-source-release.tar.gz:
	@mkdir -p dist/toree-src
	@tar -X 'etc/.src-release-ignore' -cvzf dist/toree-src/toree-$(VERSION)-source-release.tar.gz .

src-release: dist/toree-src/toree-$(VERSION)-source-release.tar.gz

dist/toree-src/toree-$(VERSION)-source-release.tar.gz.md5 dist/toree-src/toree-$(VERSION)-source-release.tar.gz.asc dist/toree-src/toree-$(VERSION)-source-release.tar.gz.sha:
	@etc/tools/./sign-file dist/toree-src/toree-$(VERSION)-source-release.tar.gz

sign-src: src-release dist/toree-src/toree-$(VERSION)-source-release.tar.gz.md5 dist/toree-src/toree-$(VERSION)-source-release.tar.gz.asc dist/toree-src/toree-$(VERSION)-source-release.tar.gz.sha

dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.md5 dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.asc dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.sha:
	@etc/tools/./sign-file dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz

sign-bin: bin-release dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.md5 dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.asc dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.sha

sign: sign-bin sign-src

audit: sign
	@etc/tools/./check-licenses
	@etc/tools/./verify-release dist/toree-bin dist/toree-src

release: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
release: PYPI_REPO?=https://pypi.python.org/pypi
release: PYPI_USER?=
release: PYPI_PASSWORD?=
release: PYPIRC=printf "[distutils]\nindex-servers =\n\tpypi\n\n[pypi]\nrepository: $(PYPI_REPO) \nusername: $(PYPI_USER)\npassword: $(PYPI_PASSWORD)" > ~/.pypirc;
release: pip-release bin-release src-release sign audit
	@$(DOCKER) $(IMAGE) bash -c '$(PYPIRC) pip install twine && \
		python setup.py register -r $(PYPI_REPO) && \
		twine upload -r pypi toree-$(VERSION).tar.gz'

define JUPYTER_COMMAND
pip install toree-$(VERSION).tar.gz
jupyter toree install --interpreters=PySpark,SQL,Scala,SparkR
cd /srv/toree/etc/examples/notebooks
jupyter notebook --ip=* --no-browser
endef

export JUPYTER_COMMAND
jupyter: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
jupyter: .example-image pip-release
	@$(DOCKER) -p 8888:8888  -e SPARK_OPTS="--master=local[4]" --user=root  $(EXAMPLE_IMAGE) bash -c "$$JUPYTER_COMMAND"
