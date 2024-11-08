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
# This Dockerfile is for MyBinder support

FROM andrewosh/binder-base

USER root

# for declarativewidgets
RUN curl -sL https://deb.nodesource.com/setup_0.12 | bash - && \
    apt-get install -y nodejs npm && \
    npm install -g bower

# for Apache Spark demos
ARG APACHE_SPARK_VERSION=3.5.2
ARG SCALA_VERSION=2.13

RUN apt-get -y update && \
    apt-get -y install software-properties-common

RUN JAVA_8=`update-alternatives --list java | grep java-8-openjdk` || echo $JAVA_8 && \
    if [ "x$JAVA_8" = "x" ]; then \
        apt-get -y update ; \
        apt-get install -y --no-install-recommends openjdk-8-jdk ca-certificates-java ; \
        apt-get clean ; \
        rm -rf /var/lib/apt/lists/* ; \
        update-ca-certificates -f ; \
        JAVA_8=`update-alternatives --list java | grep java-8-openjdk` ; \
        update-alternatives --set java $JAVA_8 ; \
    fi

RUN if [ "$SCALA_VERSION" = "2.13" ]; then APACHE_SPARK_CUSTOM_NAME=hadoop3-scala2.13; else APACHE_SPARK_CUSTOM_NAME=hadoop3; fi && \
    SPARK_TGZ_NAME=spark-${APACHE_SPARK_VERSION}-bin-${APACHE_SPARK_CUSTOM_NAME} && \
    if [ ! -d "/usr/local/$SPARK_TGZ_NAME" ]; then \
        cd /tmp ; \
        wget -q https://archive.apache.org/dist/spark/spark-${APACHE_SPARK_VERSION}/${SPARK_TGZ_NAME}.tgz ; \
        tar -xzf ${SPARK_TGZ_NAME}.tgz -C /usr/local ; \
        rm ${SPARK_TGZ_NAME}.tgz ; \
        ln -snf /usr/local/$SPARK_TGZ_NAME /usr/local/spark ; \
    fi

# R support
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    fonts-dejavu \
    gfortran \
    gcc && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV SPARK_HOME /usr/local/spark
ENV PYTHONPATH $SPARK_HOME/python:$SPARK_HOME/python/lib/py4j-0.10.9.7-src.zip
ENV PYSPARK_PYTHON /home/main/anaconda2/envs/python3/bin/python
ENV R_LIBS_USER $SPARK_HOME/R/lib

USER main

ENV TOREE_VERSION >=0.6.0.dev1, <=0.6.0

# get to the latest jupyter release and necessary libraries
RUN conda install -y jupyter seaborn futures && \
    bash -c "source activate python3 && \
        conda install seaborn"

# R packages
RUN conda config --add channels r && \
    conda install --quiet --yes \
    'r-base=3.2*' \
    'r-ggplot2=1.0*' \
    'r-rcurl=1.95*' && conda clean -tipsy

# install Toree
RUN pip install 'toree>=0.5.0.dev0, <=0.5.0'
RUN jupyter toree install --user

# include nice intro notebook
COPY index.ipynb $HOME/notebooks/
