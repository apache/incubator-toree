#  Image Properties
KERNEL_IMAGE?=spark-kernel
KERNEL_BUILD_ID?=latest
DOCKER_REGISTRY?=ignitio:5000
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
	sbt/sbt compile  kernel/pack
	sbt/sbt publish

build: kernel/target/pack/bin/sparkkernel

pack:
	docker build $(CACHE) -t $(FULL_IMAGE) .
	docker push $(FULL_IMAGE)

deploy:
	(docker rm -f $(KERNEL_CONTAINER) || true)
	docker run --net=host -d --name=$(KERNEL_CONTAINER) -e "STDIN_PORT=$(STDIN_PORT)" -e "SHELL_PORT=$(SHELL_PORT)" -e "IOPUB_PORT=$(IOPUB_PORT)" -e "CONTROL_PORT=$(CONTROL_PORT)" -e "HB_PORT=$(HB_PORT)" -e "IP=$(IP)" $(DOCKER_REGISTRY)/$(KERNEL_IMAGE):$(KERNEL_BUILD_ID)

integration:
	printf "No integration tests at the moment"

system:
	printf "No system setup at the moment"

tag:
	docker tag $(FULL_IMAGE) ${NEW_IMAGE}
	docker push ${NEW_IMAGE}
