#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.	See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.	You may obtain a copy of the License at
#
#		 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
#

.PHONY: help clean clean-dist build dev test system-test test-travis release pip-release bin-release dev-binder .binder-image audit audit-licenses

BASE_VERSION=0.2.0.dev1
VERSION=$(BASE_VERSION)-incubating
COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
ifeq (, $(findstring dev, $(VERSION)))
IS_SNAPSHOT?=false
else
IS_SNAPSHOT?=true
SNAPSHOT:=-SNAPSHOT
endif

APACHE_SPARK_VERSION?=2.0.0
SCALA_VERSION?=2.11
IMAGE?=jupyter/all-spark-notebook:07a7c4d6d447
EXAMPLE_IMAGE?=apache/toree-examples
SYSTEM_TEST_IMAGE?=apache/toree-systemtest
GPG?=/usr/local/bin/gpg
GPG_PASSWORD?=
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
printf "__version__ = '$(BASE_VERSION)'\n" >> dist/toree-pip/toree/_version.py
printf "__commit__ = '$(COMMIT)'\n" >> dist/toree-pip/toree/_version.py
endef

USE_VAGRANT?=
RUN_PREFIX=$(if $(USE_VAGRANT),vagrant ssh -c "cd $(VM_WORKDIR) && )
RUN_SUFFIX=$(if $(USE_VAGRANT),")

RUN=$(RUN_PREFIX)$(1)$(RUN_SUFFIX)

ENV_OPTS:=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

ASSEMBLY_JAR:=toree-assembly-$(VERSION)$(SNAPSHOT).jar

help:
	@echo '			audit - run audit tools against the source code'
	@echo '			clean - clean build files'
	@echo '				dev - starts ipython'
	@echo '			 dist - build a directory with contents to package'
	@echo '			build - builds assembly'
	@echo '			 test - run all units'
	@echo '		release - creates packaged distribution'
	@echo '		jupyter - starts a Jupyter Notebook with Toree installed'

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

.system-test-image:
	@-docker rm -f system_test_image
	@docker run -it --user root --name system_test_image \
		$(IMAGE) bash -c "cd /tmp && \
			wget http://apache.claz.org/spark/spark-$(APACHE_SPARK_VERSION)/spark-$(APACHE_SPARK_VERSION)-bin-hadoop2.6.tgz && \
			tar xzf spark-$(APACHE_SPARK_VERSION)-bin-hadoop2.6.tgz -C /usr/local && \
			rm spark-$(APACHE_SPARK_VERSION)-bin-hadoop2.6.tgz && \
			cd /usr/local && \
			rm spark && \
			ln -s spark-$(APACHE_SPARK_VERSION)-bin-hadoop2.6 spark && \
			echo /usr/local/spark/RELEASE && \
			\
			echo \"===> add webupd8 repository...\"	&& \
			echo \"deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main\" | tee /etc/apt/sources.list.d/webupd8team-java.list	&& \
			echo \"deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main\" | tee -a /etc/apt/sources.list.d/webupd8team-java.list	&& \
			apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886	&& \
			apt-get update && \
			\
			echo \"===> install Java\"	&& \
			echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections	&& \
			echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections	&& \
			DEBIAN_FRONTEND=noninteractive	apt-get install -y --force-yes oracle-java8-installer oracle-java8-set-default && \
			apt-get clean && \
			update-java-alternatives -s java-8-oracle"
	@docker commit system_test_image $(SYSTEM_TEST_IMAGE)
	@-docker rm -f system_test_image
	touch $@

.binder-image:
	@docker build --rm -t $(BINDER_IMAGE) .

dev-binder: .binder-image
	@docker run --rm -it -p 8888:8888	\
		-v `pwd`:/home/main/notebooks \
		--workdir /home/main/notebooks $(BINDER_IMAGE) \
		/home/main/start-notebook.sh --ip=0.0.0.0

target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): VM_WORKDIR=/src/toree-kernel
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): dist/toree-legal project/build.properties build.sbt project/common.scala project/plugins.sbt
	$(call RUN,$(ENV_OPTS) sbt toree/assembly)

build: target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR)

dev: DOCKER_WORKDIR=/srv/toree/etc/examples/notebooks
dev: SUSPEND=n
dev: DEBUG_PORT=5005
dev: .example-image dist
	@$(DOCKER) \
		-e SPARK_OPTS="--master=local[4] --driver-java-options=-agentlib:jdwp=transport=dt_socket,server=y,suspend=$(SUSPEND),address=5005" \
		-v `pwd`/etc/kernel.json:/usr/local/share/jupyter/kernels/toree/kernel.json \
		-p $(DEBUG_PORT):5005 -p 8888:8888 \
		--user=root	$(EXAMPLE_IMAGE) \
		bash -c "cp -r /srv/toree/dist/toree/* /usr/local/share/jupyter/kernels/toree/. \
			&& jupyter notebook --ip=* --no-browser"

test: VM_WORKDIR=/src/toree-kernel
test:
	$(call RUN,$(ENV_OPTS) JAVA_OPTS="-Xmx4096M" sbt compile test)

sbt-%:
	$(call RUN,$(ENV_OPTS) sbt $(subst sbt-,,$@) )

dist/toree/lib: target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR)
	@mkdir -p dist/toree/lib
	@cp target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR) dist/toree/lib/.

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

dist/toree-legal/NOTICE: NOTICE etc/legal/NOTICE_extras
	@mkdir -p dist/toree-legal
	@cat NOTICE > dist/toree-legal/NOTICE
	@echo '\n' >> dist/toree-legal/NOTICE
	@cat etc/legal/NOTICE_extras >> dist/toree-legal/NOTICE

dist/toree-legal/DISCLAIMER:
	@mkdir -p dist/toree-legal
	@cp DISCLAIMER dist/toree-legal/DISCLAIMER

dist/toree-legal: dist/toree-legal/LICENSE dist/toree-legal/NOTICE dist/toree-legal/DISCLAIMER
	@cp -R etc/legal/licenses dist/toree-legal/.

dist/toree: dist/toree/VERSION dist/toree-legal dist/toree/lib dist/toree/bin RELEASE_NOTES.md
	@cp -R dist/toree-legal/* dist/toree
	@cp RELEASE_NOTES.md dist/toree/RELEASE_NOTES.md

dist: dist/toree

define JUPYTER_COMMAND
pip install toree-$(BASE_VERSION).tar.gz
jupyter toree install --interpreters=PySpark,SQL,Scala,SparkR
cd /srv/toree/etc/examples/notebooks
jupyter notebook --ip=* --no-browser
endef

export JUPYTER_COMMAND
jupyter: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
jupyter: .example-image pip-release
	@$(DOCKER) -p 8888:8888	-e SPARK_OPTS="--master=local[4]" --user=root	$(EXAMPLE_IMAGE) bash -c "$$JUPYTER_COMMAND"

################################################################################
# System Tests Using Jupyter Kernel Test (https://github.com/jupyter/jupyter_kernel_test)
################################################################################
system-test: pip-release .system-test-image
	@echo '-- Running jupyter kernel tests'
	@docker run -ti --rm \
		--name jupyter_kernel_tests \
		-v `pwd`/dist/toree-pip:/srv/toree-pip \
		-v `pwd`/test_toree.py:/srv/test_toree.py \
		-v `pwd`/scala-interpreter/src/test/resources:/srv/system-test-resources \
		--user=root \
		$(SYSTEM_TEST_IMAGE) \
		bash -c "(cd /srv/system-test-resources && python -m http.server 8000 &) && \
		rm -rf /home/jovyan/.local/share/jupyter/kernels/apache_toree_scala/ && \
		pip install /srv/toree-pip/toree*.tar.gz && jupyter toree install --interpreters=PySpark,Scala,SparkR && \
		pip install nose jupyter_kernel_test && python /srv/test_toree.py"


################################################################################
# Jars
################################################################################
publish-jars:
	@$(ENV_OPTS) GPG_PASSWORD='$(GPG_PASSWORD)' GPG=$(GPG) sbt publish-signed

################################################################################
# PIP PACKAGE
################################################################################
dist/toree-pip/toree-$(BASE_VERSION).tar.gz: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
dist/toree-pip/toree-$(BASE_VERSION).tar.gz: dist/toree
	@mkdir -p dist/toree-pip
	@cp -r dist/toree dist/toree-pip
	@cp dist/toree/LICENSE dist/toree-pip/LICENSE
	@cp dist/toree/NOTICE dist/toree-pip/NOTICE
	@cp dist/toree/DISCLAIMER dist/toree-pip/DISCLAIMER
	@cp dist/toree/VERSION dist/toree-pip/VERSION
	@cp dist/toree/RELEASE_NOTES.md dist/toree-pip/RELEASE_NOTES.md
	@cp -R dist/toree/licenses dist/toree-pip/licenses
	@cp -rf etc/pip_install/* dist/toree-pip/.
	@$(GEN_PIP_PACKAGE_INFO)
	@$(DOCKER) --user=root $(IMAGE) python setup.py sdist --dist-dir=.
	@$(DOCKER) -p 8888:8888 --user=root	$(IMAGE) bash -c	'pip install toree-$(BASE_VERSION).tar.gz && jupyter toree install'
#	-@(cd dist/toree-pip; find . -not -name 'toree-$(VERSION).tar.gz' -maxdepth 1 | xargs rm -r )

pip-release: dist/toree-pip/toree-$(BASE_VERSION).tar.gz

dist/toree-pip/toree-$(BASE_VERSION).tar.gz.md5 dist/toree-pip/toree-$(BASE_VERSION).tar.gz.asc dist/toree-pip/toree-$(BASE_VERSION).tar.gz.sha: dist/toree-pip/toree-$(BASE_VERSION).tar.gz
	@GPG_PASSWORD='$(GPG_PASSWORD)' GPG=$(GPG) etc/tools/./sign-file dist/toree-pip/toree-$(BASE_VERSION).tar.gz

sign-pip: dist/toree-pip/toree-$(BASE_VERSION).tar.gz.md5 dist/toree-pip/toree-$(BASE_VERSION).tar.gz.asc dist/toree-pip/toree-$(BASE_VERSION).tar.gz.sha

publish-pip: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
publish-pip: PYPI_REPO?=https://pypi.python.org/pypi
publish-pip: PYPI_USER?=
publish-pip: PYPI_PASSWORD?=
publish-pip: PYPIRC=printf "[distutils]\nindex-servers =\n\tpypi\n\n[pypi]\nrepository: $(PYPI_REPO) \nusername: $(PYPI_USER)\npassword: $(PYPI_PASSWORD)" > ~/.pypirc;
publish-pip: sign-pip
	@$(DOCKER) $(IMAGE) bash -c '$(PYPIRC) pip install twine && \
		python setup.py register -r $(PYPI_REPO) && \
		twine upload -r pypi toree-$(BASE_VERSION).tar.gz toree-$(BASE_VERSION).tar.gz.asc'

################################################################################
# BIN PACKAGE
################################################################################
dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz: dist/toree
	@mkdir -p dist/toree-bin
	@(cd dist; tar -cvzf toree-bin/toree-$(VERSION)-binary-release.tar.gz toree)

bin-release: dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz

dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.md5 dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.asc dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.sha: dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz
	@GPG_PASSWORD='$(GPG_PASSWORD)' GPG=$(GPG) etc/tools/./sign-file dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz

sign-bin: dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.md5 dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.asc dist/toree-bin/toree-$(VERSION)-binary-release.tar.gz.sha

publish-bin:

################################################################################
# SRC PACKAGE
################################################################################
dist/toree-src/toree-$(VERSION)-source-release.tar.gz:
	@mkdir -p dist/toree-src
	@tar -X 'etc/.src-release-ignore' -cvzf dist/toree-src/toree-$(VERSION)-source-release.tar.gz .

src-release: dist/toree-src/toree-$(VERSION)-source-release.tar.gz

dist/toree-src/toree-$(VERSION)-source-release.tar.gz.md5 dist/toree-src/toree-$(VERSION)-source-release.tar.gz.asc dist/toree-src/toree-$(VERSION)-source-release.tar.gz.sha: dist/toree-src/toree-$(VERSION)-source-release.tar.gz
	@GPG_PASSWORD='$(GPG_PASSWORD)' GPG=$(GPG) etc/tools/./sign-file dist/toree-src/toree-$(VERSION)-source-release.tar.gz

sign-src: dist/toree-src/toree-$(VERSION)-source-release.tar.gz.md5 dist/toree-src/toree-$(VERSION)-source-release.tar.gz.asc dist/toree-src/toree-$(VERSION)-source-release.tar.gz.sha

publish-src:

################################################################################
# ALL PACKAGES
################################################################################
release: pip-release src-release bin-release sign

sign: sign-bin sign-src sign-pip

audit-licenses:
	@etc/tools/./check-licenses

audit: sign audit-licenses
	@etc/tools/./verify-release dist/toree-bin dist/toree-src dist/toree-pip

publish: audit publish-bin publish-pip publish-src publish-jars

all: clean test system-test audit

all-travis: clean test system-test audit-licenses

clean-travis:
	find $(HOME)/.sbt -name "*.lock" | xargs rm
	find $(HOME)/.ivy2 -name "ivydata-*.properties" | xargs rm
