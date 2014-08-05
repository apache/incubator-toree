root=/ETSparkProjects/SparkKernel/scripts

function createCluster(){
	sudo ${root}/docker-scripts/deploy/deploy.sh -i spark:1.0.0 -w 3 -v /vagrant/data/
	echo address=\"/host-box/172.17.42.1\" | sudo tee -a  /tmp/$(ls /tmp | grep dnsdir)/0hosts
}

function terminateCluster(){
	sudo ${root}/docker-scripts/deploy/kill_all.sh spark
	sudo ${root}/docker-scripts/deploy/kill_all.sh nameserver	
	sudo rm -r /tmp/dnsdir*
}

function dkill(){
	docker kill $1 && docker rm $1
}

function dclean(){
	docker rm `docker ps -a | awk '{print $1}' | tail -n +2`
	docker rmi `docker images | grep ^\<none | awk '{print $3}'`
}

function build(){
	mvn -f ${root}/pom.xml package -Dmaven.test.skip=true
}

function fixDNS(){
	ns=$(docker inspect `docker ps | grep dns | awk '{print $1}'` | grep IPAddress | cut -d '"' -f 4)
	echo "nameserver ${ns}" | cat - /etc/resolv.conf > /tmp/resolv.conf && sudo mv /tmp/resolv.conf /etc/resolv.conf
}

function loadJars(){
  docker run -t -i --entrypoint="/project/bin/copyToHdfs.sh" -v ${root}:/project spark-worker:1.0.0
}

function killDriver(){
  docker run -t -i --entrypoint="/opt/spark-1.0.0/bin/spark-class" spark-worker:1.0.0 org.apache.spark.deploy.Client kill spark://master:7077 $1
}

function getLogs(){
  docker cp $1:/opt/spark-1.0.0-bin-hadoop1/work .
}
