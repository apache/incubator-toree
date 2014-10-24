#! /bin/bash

ps aux | grep "SparkKernel" | grep -v grep | awk '{print $2}' | xargs kill -9
