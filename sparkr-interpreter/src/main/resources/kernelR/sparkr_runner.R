#
# Copyright 2015 IBM Corp.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# Initialize our global environment
.runnerEnv <- new.env()

# Set our script to have its working directory where it currently resides
# http://stackoverflow.com/questions/1815606/rscript-determine-path-of-the-executing-script
initial.options <- commandArgs(trailingOnly = FALSE)
file.arg.name <- "--file="
script.name <- sub(
  file.arg.name,
  "",
  initial.options[grep(file.arg.name, initial.options)]
)
script.basename <- dirname(script.name)
setwd(script.basename)

# TODO: Use this library instead of the forked SparkR once they either
#       a) allow us to connect and use an existing Spark Context
#       b) allow us to have access to the .sparkREnv to do our own work
#
#       and provide access in some form to the methods used to access the JVM
# Add the SparkR library to our list
#.libPaths(c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib"), .libPaths()))
install.packages("sparkr_bundle.tar.gz", repos = NULL, type="source")
library(SparkR)

# Bring in other dependencies not exposed in standard SparkR
source("sparkr_runner_utils.R")

# Connect to the backend
sparkR.connect()

# Retrieve the bridge used to perform actions on the JVM
bridge <- callJStatic(
  "org.apache.toree.kernel.interpreter.sparkr.SparkRBridge", "sparkRBridge"
)

# Retrieve the state used to pull code off the JVM and push results back
state <- callJMethod(bridge, "state")

# Acquire the kernel API instance to expose
kernel <- callJMethod(bridge, "kernel")
assign("kernel", kernel, .runnerEnv)

# Acquire the SparkContext instance to expose
#sc <- callJMethod(bridge, "javaSparkContext")
#assign("sc", sc, .runnerEnv)
sc <- NULL

# Acquire the SQLContext instance to expose
#sqlContext <- callJMethod(bridge, "sqlContext")
#sqlContext <- callJMethod(kernel, "sqlContext")
#assign("sqlContext", sqlContext, .runnerEnv)

# TODO: Is there a way to control input/output (maybe use sink)
repeat {
  # Load the conainer of the code
  codeContainer <- callJMethod(state, "nextCode")

  # If not valid result, wait 1 second and try again
  if (!class(codeContainer) == "jobj") {
    Sys.sleep(1)
    next()
  }

  # Retrieve the code id (for response) and code
  codeId <- callJMethod(codeContainer, "codeId")
  code <- callJMethod(codeContainer, "code")

  if (is.null(sc)) {
    sc <- callJMethod(kernel, "javaSparkContext")
    if(!is.null(sc)) {
      assign("sc", sc, .runnerEnv)
      sqlContext <- callJMethod(kernel, "sqlContext")
      assign("sqlContext", sqlContext, .runnerEnv)
    }
  }
  print(paste("Received Id", codeId, "Code", code))

  # Parse the code into an expression to be evaluated
  codeExpr <- parse(text = code)
  print(paste("Code expr", codeExpr))

  tryCatch({
    # Evaluate the code provided and capture the result as a string
    result <- capture.output(eval(codeExpr, envir = .runnerEnv))
    print(paste("Result type", class(result), length(result)))
    print(paste("Success", codeId, result))

    # Mark the execution as a success and send back the result
    # If output is null/empty, ensure that we can send it (otherwise fails)
    if (is.null(result) || length(result) <= 0) {
      print("Marking success with no output")
      callJMethod(state, "markSuccess", codeId)
    } else {
      # Clean the result before sending it back
      cleanedResult <- trimws(flatten(result, shouldTrim = FALSE))

      print(paste("Marking success with output:", cleanedResult))
      callJMethod(state, "markSuccess", codeId, cleanedResult)
    }
  }, error = function(ex) {
    # Mark the execution as a failure and send back the error
    print(paste("Failure", codeId, toString(ex)))
    callJMethod(state, "markFailure", codeId, toString(ex))
  })
}
