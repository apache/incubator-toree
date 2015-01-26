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

#  Image Properties
KERNEL_IMAGE?=spark-kernel
KERNEL_BUILD_ID?=latest
DOCKER_REGISTRY?=com.ibm.spark:5000
FULL_IMAGE?=$(DOCKER_REGISTRY)/$(KERNEL_IMAGE):$(KERNEL_BUILD_ID)
CACHE?="--no-cache"

#   Container Properties
KERNEL_CONTAINER?=spark-kernel
STDIN_PORT?=48000
SHELL_PORT?=48001
IOPUB_PORT?=48002
CONTROL_PORT?=48003
HB_PORT?=48004
IP?=0.0.0.0

clean:
	sbt clean

kernel/target/pack/bin/sparkkernel:
	sbt compile  kernel/pack

build: kernel/target/pack/bin/sparkkernel

build-image:
	docker build $(CACHE) -t $(FULL_IMAGE) .

pack: build-image
	sbt publish
	docker push $(FULL_IMAGE)

deploy:
	(docker rm -f $(KERNEL_CONTAINER) || true)
	docker run -d \
	    --name=$(KERNEL_CONTAINER) \
	    -e "STDIN_PORT=$(STDIN_PORT)" \
	    -e "SHELL_PORT=$(SHELL_PORT)" \
	    -e "IOPUB_PORT=$(IOPUB_PORT)" \
	    -e "CONTROL_PORT=$(CONTROL_PORT)" \
	    -e "HB_PORT=$(HB_PORT)" -e "IP=$(IP)" \
	    $(FULL_IMAGE)

integration:
	printf "No integration tests at the moment"

system:
	printf "No system setup at the moment"

tag:
	docker tag $(FULL_IMAGE) ${NEW_IMAGE}
	docker push ${NEW_IMAGE}

tag-latest:
	docker tag $(FULL_IMAGE) $(DOCKER_REGISTRY)/$(KERNEL_IMAGE):latest
	docker push $(DOCKER_REGISTRY)/$(KERNEL_IMAGE):latest
