# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<-SCRIPT

sudo apt-get update
sudo apt-get -y install openjdk-7-jdk maven wget build-essential git uuid-dev


# Install IPython and ZeroMQ
sudo apt-get -f -y install
sudo apt-get -y install python-pip python-dev libzmq-dev
sudo pip install -e ".[notebook]" --user
sudo pip install pyzmq==2.1.11
sudo pip install jinja2
sudo pip install tornado
sudo pip install jsonschema
sudo pip install runipy

sudo apt-get -y install git
cd /ETSparkProjects
git clone --recursive https://github.com/ipython/ipython.git
cd ipython
sudo python setup.py install

if [ -z `which docker` ]; then
  curl -sSL https://get.docker.io/ubuntu/ | sudo sh
  sudo gpasswd -a vagrant docker
  sudo service docker stop
  sudo chown vagrant /var/run/docker.sock
  sudo service docker start
fi

echo "vagrant:vagrant"|chpasswd


#	Install scala and sbt
cd /tmp
wget --progress=bar:force http://www.scala-lang.org/files/archive/scala-2.10.4.deb
sudo dpkg -i scala-2.10.4.deb
rm scala-2.10.4.deb
wget --progress=bar:force http://dl.bintray.com/sbt/debian/sbt-0.13.5.deb
sudo dpkg -i sbt-0.13.5.deb
rm sbt-0.13.5.deb

# Add Spark Kernel json to IPython configuration
echo "Adding kernel.json"
mkdir -p /home/vagrant/.ipython/kernels/spark
chown -R vagrant.vagrant /home/vagrant/.ipython
cat << EOF > /home/vagrant/.ipython/kernels/spark/kernel.json
{
    "display_name": "Spark 1.0.1 (Scala 2.10.4)",
    "language": "scala",
    "argv": [
        "/home/vagrant/local/bin/sparkkernel",
        "--profile",
        "{connection_file}"
    ],
    "codemirror_mode": "scala"
}
EOF

# Install Kafka & Zookeeper with Docker setup
# See http://wurstmeister.github.io/kafka-docker/
export START_SCRIPT=https://raw2.github.com/wurstmeister/kafka-docker/master/start-broker.sh
until curl -Ls $START_SCRIPT | bash /dev/stdin 1 49899 192.168.44.44; do
    printf "Trying to install kafka and zookeeper docker container again...."
done

# Install Cassandra with Docker setup
cd /opt/
git clone https://github.com/nicolasff/docker-cassandra.git
cd docker-cassandra
sudo cp install/bin/pipework /usr/bin
until make image VERSION=2.0.10; do
    printf "Trying to install cassandra docker container again...."
done

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
    vb.cpus = 2

    # Expand the memory.
    vb.customize ["modifyvm", :id, "--memory", "2048"]
  end
end
