#!/bin/sh -x

# SparkR information relative to this script
SPARKR_DIR="R"
SPARKR_BUILD_COMMAND="sh install-dev.sh"
SPARKR_LIB_OUTPUT_DIR="lib"
SPARKR_PACKAGE_DIR="SparkR"

# Output information
SPARKR_BUNDLE="sparkr_bundle.tar.gz"

# Build and package our SparkR bundle
(cd $SPARKR_DIR && $SPARKR_BUILD_COMMAND)
(cd $SPARKR_DIR && \
 cd $SPARKR_LIB_OUTPUT_DIR && \
 tar -zcf $SPARKR_BUNDLE $SPARKR_PACKAGE_DIR)
mv $SPARKR_DIR/$SPARKR_LIB_OUTPUT_DIR/$SPARKR_BUNDLE .

# Clean up the library output
rm -rf $SPARKR_DIR/$SPARKR_LIB_OUTPUT_DIR
