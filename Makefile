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

.PHONY: clean build build-image dev vagrantup

#   Container Properties
KERNEL_CONTAINER?=spark-kernel
STDIN_PORT?=48000
SHELL_PORT?=48001
IOPUB_PORT?=48002
CONTROL_PORT?=48003
HB_PORT?=48004
IP?=0.0.0.0

clean:
	vagrant ssh -c "cd /src/spark-kernel/ && sbt clean"

kernel/target/pack/bin/sparkkernel: vagrantup ${shell find ./*/src/main/**/*}
	vagrant ssh -c "cd /src/spark-kernel/ && sbt compile && sbt pack"
	vagrant ssh -c "cd /src/spark-kernel/kernel/target/pack && make install"

build-image: IMAGE_NAME?cloudet/spark-kernel
build-image: CACHE?=""
build-image:
	vagrant ssh -c "cd /src/spark-kernel && docker build $(CACHE) -t $(FULL_IMAGE) ."

run-image: KERNEL_CONTAINER?=spark-kernel
run-image: STDIN_PORT?=48000
run-image: SHELL_PORT?=48001
run-image: IOPUB_PORT?=48002
run-image: CONTROL_PORT?=48003
run-image: HB_PORT?=48004
run-image: IP?=0.0.0.0
run-image: build-image
	vagrant ssh -c "docker rm -f $(KERNEL_CONTAINER) || true"
	vagrant ssh -c "docker run -d \
											--name=$(KERNEL_CONTAINER) \
											-e "STDIN_PORT=$(STDIN_PORT)" \
											-e "SHELL_PORT=$(SHELL_PORT)" \
											-e "IOPUB_PORT=$(IOPUB_PORT)" \
											-e "CONTROL_PORT=$(CONTROL_PORT)" \
											-e "HB_PORT=$(HB_PORT)" -e "IP=$(IP)" \
											$(FULL_IMAGE)"

vagrantup:
	vagrant up

build: kernel/target/pack/bin/sparkkernel

dev: build
	vagrant ssh -c "cd ~ && ipython notebook --ip=* --no-browser"

test: build
	vagrant ssh -c "cd /src/spark-kernel/ && sbt test"