# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<-SCRIPT
if [ -z `which docker` ]; then
  sudo apt-get update
  sudo apt-get -y install docker.io
  sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 36A1D7869245C8950F966E92D8576A8BA88D21E9
  sudo update-alternatives --install /usr/bin/docker docker /usr/bin/docker.io 50
  sudo cp -a /etc/bash_completion.d/docker{.io,}
  sudo sed -i 's/\(docker\)\.io/\1/g' /etc/bash_completion.d/docker
  sudo ln -s /usr/share/man/man1/docker{.io,}.1.gz
  sudo usermod -a -G docker vagrant
  sudo service docker.io stop
  sudo sed -i /etc/init/docker.io.conf -e 's#DOCKER_OPTS=#DOCKER_OPTS="-H tcp://0.0.0.0:4243 -H unix:///var/run/docker.sock"#'
  sudo service docker.io start
fi

echo "vagrant:vagrant"|chpasswd
#	Install java and maven
sudo apt-get -y install openjdk-7-jdk maven wget build-essential uuid-dev

#	Install ZeroMQ
mkdir /zermoq
cd /zermoq
wget http://download.zeromq.org/zeromq-2.2.0.tar.gz
tar xzf zeromq-2.2.0.tar.gz
./configure && make && make install
ldconfig

#	Install scala and sbt
cd /tmp
wget http://www.scala-lang.org/files/archive/scala-2.10.4.deb
sudo dpkg -i scala-2.10.4.deb
rm scala-2.10.4.deb
wget http://dl.bintray.com/sbt/debian/sbt-0.13.5.deb
sudo dpkg -i sbt-0.13.5.deb
rm sbt-0.13.5.deb

# Install IPython
sudo apt-get -f -y install 
sudo apt-get -y install python-pip python-dev
cd /ETSparkProjects
git clone --recursive https://github.com/ipython/ipython.git
cd ipython
sudo python setup.py install
sudo pip install -e ".[notebook]" --user

SCRIPT

Vagrant.configure("2") do |config|
  # Have the script install docker
  config.vm.provision :shell, :inline => $script

  # Every Vagrant virtual environment requires a box to build off of.
  config.vm.box = "trusty_ubuntu"
  config.vm.box_url = "https://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box"
  config.vm.hostname = "host-box"

  # Lets ubuntu reach the intranet when using vpn
  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
  end

  # Mount the directory containing this file as /vagrant in the VM.
  # Since this file is copied around we need to figure out where the docker files are

  config.vm.synced_folder "./" , "/ETSparkProjects/SparkKernel"

  # Create a private network, which allows host-only access to the machine
  # using a specific IP. Make sure this IP doesn't exist on your local network.
  config.vm.network :private_network, ip: "192.168.44.44"

  # Forward all Docker ports to localhost if set.
  if ENV['EXPOSE_DOCKER']
    (49000..49900).each do |port|
      config.vm.network :forwarded_port, :host => port, :guest => port
    end
  end

  config.vm.provider :virtualbox do |vb|
    # Expand the memory.
    vb.customize ["modifyvm", :id, "--memory", "2048"]
  end
end
