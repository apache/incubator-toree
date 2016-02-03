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

# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<-SCRIPT
mkdir -p /var/lib/vagrant_dev_install_flags
function flag_is_set() {
  if [ -e /var/lib/vagrant_dev_install_flags/$1 ]; then
    return 0
  else
    return 1
  fi
}

function set_flag() {
  touch /var/lib/vagrant_dev_install_flags/$1
}

function unset_flag() {
  rm -f /var/lib/vagrant_dev_install_flags/$1
}

function unset_all_flags() {
  rm -f /var/lib/vagrant_dev_install_flags/*
}

# Set vagrant user pw
printf "vagrant:vagrant\n" | chpasswd

# Update before we go
apt-get update

# Install Java and other dependencies
if ! flag_is_set CORE_DEPS; then
  apt-get -y install openjdk-7-jdk maven wget build-essential git uuid-dev && \
  set_flag CORE_DEPS
fi

# Install IPython and ZeroMQ
IPYTHON_VERSION=3.2.1

if ! flag_is_set IPYTHON; then
  apt-get -f -y install && \
  apt-get -y install python3-pip python-dev libzmq-dev build-essential && \
  cd /src && \
  pip3 install ipython[notebook]==${IPYTHON_VERSION} && \
  ipython profile create && \
  set_flag IPYTHON
fi

if [ -z `which docker` ]; then
  sudo apt-get update -y
  sudo apt-get -y install apt-transport-https linux-image-extra-`uname -r`
  printf "deb https://get.docker.com/ubuntu docker main\n" > /etc/apt/sources.list.d/docker.list
  sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 36A1D7869245C8950F966E92D8576A8BA88D21E9
  sudo apt-get update -y
  sudo apt-get install -y lxc-docker-1.4.1
  sudo chown vagrant:vagrant /var/run/docker.sock
fi

# Installing R
if ! flag_is_set R; then
  sudo sh -c 'printf "deb http://cran.cnr.Berkeley.edu/bin/linux/ubuntu trusty/" >> /etc/apt/sources.list' && \
  sudo apt-get update -y && \
  sudo apt-get install r-base r-base-dev && \
  sudo chmod 777 /usr/local/lib/R/site-library && \
  set_flag R
fi

# Install scala and sbt (if not already installed)
cd /tmp

# If Scala is not installed, install it
if ! flag_is_set SCALA; then
  apt-get install -f -y && \
  apt-get install -y libjansi-java && \
  apt-get install -f -y && \
  wget --progress=bar:force http://www.scala-lang.org/files/archive/scala-2.10.4.deb && \
  dpkg -i scala-2.10.4.deb && \
  rm scala-2.10.4.deb && \
  set_flag SCALA
fi

# If sbt is not installed, install it
if ! flag_is_set SBT; then
  wget --progress=bar:force http://dl.bintray.com/sbt/debian/sbt-0.13.9.deb && \
  dpkg -i sbt-0.13.9.deb && \
  rm sbt-0.13.9.deb && \
  set_flag SBT
fi

# Downloading Spark
SPARK_VERSION=1.5.1
if ! flag_is_set SPARK; then
  cd /opt && \
  wget http://apache.arvixe.com/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop2.3.tgz && \
  tar xvzf spark-${SPARK_VERSION}-bin-hadoop2.3.tgz && \
  ln -s spark-${SPARK_VERSION}-bin-hadoop2.3 spark && \
  export SPARK_HOME=/opt/spark && \
  set_flag SPARK
fi

# Add Spark Kernel json to IPython configuration
echo "Adding kernel.json"
mkdir -p /home/vagrant/.ipython/kernels/toree-kernel
cat << EOF > /home/vagrant/.ipython/kernels/toree-kernel/kernel.json
{
    "display_name": "Toree",
    "language_info": { "name": "scala" },
    "argv": [
        "/src/toree-kernel/dist/toree/bin/run.sh",
        "--profile",
        "{connection_file}"
    ],
    "codemirror_mode": "scala",
    "env": {
        "SPARK_OPTS": "--driver-java-options=-Xms1024M --driver-java-options=-Xmx4096M --driver-java-options=-Dlog4j.logLevel=trace",
        "MAX_INTERPRETER_THREADS": "16",
        "SPARK_CONFIGURATION": "spark.cores.max=4",
        "CAPTURE_STANDARD_OUT": "true",
        "CAPTURE_STANDARD_ERR": "true",
        "SEND_EMPTY_OUTPUT": "false",
        "SPARK_HOME": "/opt/spark",
        "PYTHONPATH": "/opt/spark/python:/opt/spark/python/lib/py4j-0.8.2.1-src.zip"
     }
}
EOF

# Add Scala syntax highlighting support to custom.js of default profile
printf "Appending to profile_default custom.js\n"
(su vagrant
mkdir -p /home/vagrant/.ipython/profile_default/static/custom/
cat << EOF >> /home/vagrant/.ipython/profile_default/static/custom/custom.js
CodeMirror.requireMode('clike',function(){
    "use strict";

    CodeMirror.defineMode("scala", function(conf, parserConf) {
        var scalaConf = {};
        for (var prop in parserConf) {
            if (parserConf.hasOwnProperty(prop)) {
                scalaConf[prop] = parserConf[prop];
            }
        }

        scalaConf.name = 'text/x-scala';

        var mode = CodeMirror.getMode(conf, scalaConf);

        return mode;
    }, 'scala');

    CodeMirror.defineMIME("text/x-spark", "spark", "scala");
})
EOF
)

chown -R vagrant.vagrant /home/vagrant/.ipython

SCRIPT

Vagrant.configure("2") do |config|
  # Have the script install docker
  config.vm.provision :shell, :inline => $script

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "trusty_ubuntu"
  config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box"
  config.vm.hostname = "host-box"

  # Mount the directory containing this file as /vagrant in the VM.
  # Since this file is copied around we need to figure out where the docker files are
  config.vm.synced_folder "./" , "/src/toree-kernel"

  # Create a private network, which allows host-only access to the machine
  # using a specific IP. Make sure this IP doesn't exist on your local network.
  config.vm.network :private_network, ip: "192.168.44.44"
  config.vm.network :forwarded_port, guest: 22, host: 2223

  # Configure memory and cpus
  config.vm.provider :virtualbox do |vb|
    vb.customize ["modifyvm", :id, "--memory", "2048"]
    vb.customize ["modifyvm", :id, "--cpus", "2"]
    vb.name = "toree-kernel-vm"

  end
end
