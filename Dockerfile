FROM ubuntu:14.04
#   Setup
RUN gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys E56151BF
RUN echo deb http://repos.mesosphere.io/$(lsb_release -is | tr '[:upper:]' '[:lower:]') $(lsb_release -cs) main >> /etc/apt/sources.list.d/mesosphere.list
RUN apt-get update
RUN apt-get --no-install-recommends -y --force-yes install openjdk-7-jre mesos=0.20.1-1.0.ubuntu1404 libzmq-dev
ENV MESOS_NATIVE_LIBRARY /usr/local/lib/libmesos.so

#   Setup the binary we will run
ENTRYPOINT /app/bin/sparkkernel

#   Install the pack elements
ADD kernel/target/pack /app
RUN chmod +x /app/bin/sparkkernel
