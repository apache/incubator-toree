# This Dockerfile is for MyBinder support

FROM andrewosh/binder-base

USER root

# for declarativewidgets
RUN curl -sL https://deb.nodesource.com/setup_0.12 | bash - && \
    apt-get install -y nodejs npm && \
    npm install -g bower

# for pyspark demos
ENV APACHE_SPARK_VERSION 1.6.1
RUN apt-get -y update && \
    apt-get install -y --no-install-recommends openjdk-7-jre-headless && \
    apt-get clean
RUN cd /tmp && \
        wget -q http://apache.claz.org/spark/spark-${APACHE_SPARK_VERSION}/spark-${APACHE_SPARK_VERSION}-bin-hadoop2.6.tgz && \
        tar xzf spark-${APACHE_SPARK_VERSION}-bin-hadoop2.6.tgz -C /usr/local && \
        rm spark-${APACHE_SPARK_VERSION}-bin-hadoop2.6.tgz
RUN cd /usr/local && ln -s spark-${APACHE_SPARK_VERSION}-bin-hadoop2.6 spark

# R support
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    fonts-dejavu \
    gfortran \
    gcc && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV SPARK_HOME /usr/local/spark
ENV PYTHONPATH $SPARK_HOME/python:$SPARK_HOME/python/lib/py4j-0.9-src.zip
ENV PYSPARK_PYTHON /home/main/anaconda2/envs/python3/bin/python
ENV R_LIBS_USER $SPARK_HOME/R/lib

USER main

ENV DASHBOARDS_VERSION ==0.4.1
ENV DASHBOARDS_BUNDLERS_VERSION ==0.2.2

ENV TOREE_VERSION >=0.1.0.dev0, <=0.1.0

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

ENV DECL_WIDGETS_VERSION 0.4.3

# install incubator extensions
RUN pip install jupyter_dashboards==0.4.1 \
    jupyter_declarativewidgets==$DECL_WIDGETS_VERSION \
    jupyter_dashboards_bundlers==0.2.2
RUN jupyter dashboards install --user --symlink && \
    jupyter declarativewidgets install --user --symlink && \
    jupyter dashboards activate && \
    jupyter declarativewidgets activate && \
    jupyter dashboards_bundlers activate

# install kernel-side incubator extensions for python3 environment too
RUN bash -c "source activate python3 && pip install jupyter_declarativewidgets==$DECL_WIDGETS_VERSION"

# install Toree
RUN pip install 'toree>=0.1.0.dev0, <=0.1.0'
RUN jupyter toree install --user


# include nice intro notebook
COPY index.ipynb $HOME/notebooks/