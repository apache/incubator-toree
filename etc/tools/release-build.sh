#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

function exit_with_usage {
  cat << EOF

release-build - Creates build distributions from a git commit hash or from HEAD.

SYNOPSIS

usage: release-build.sh [--release-prepare | --release-publish]

DESCRIPTION

Prepare and publish Apache Toree release artifacts.

--release-prepare builds the release locally without pushing to any external
system. Artifacts are left in target/toree/dist/ for review.

--release-publish publishes a previously prepared release: pushes git commits
and tags, stages artifacts to Apache SVN dist/dev, and deploys the assembly
jar to the Apache Maven staging repository.

--release-prepare --releaseVersion="0.6.0" --developmentVersion="0.7.0.dev0" [--releaseRc="rc1"] [--tag="v0.6.0-incubating-rc1"] [--gitCommitHash="a874b73"] [--dryRun]
Prepare a release locally: clone, version bump, tag, and build all artifacts.
Use --dryRun to test version bump logic without building.

--release-publish --releaseVersion="0.6.0" [--releaseRc="rc1"] [--tag="v0.6.0-incubating-rc1"]
Publish a previously prepared release. Requires --release-prepare to have been
run first. Does NOT support --dryRun.

OPTIONS

--releaseVersion     - Release identifier used when publishing
--developmentVersion - Release identifier used for next development cycle
--releaseRc          - Release RC identifier used when publishing, default 'rc1'
--tag                - Release Tag identifier used when tagging the release, default 'v\$releaseVersion-incubating-\$releaseRc'
--gitCommitHash      - Commit to build from, default master HEAD (prepare only)
--dryRun             - Dry run only, skips the build step (prepare only)

A GPG passphrase is expected as an environment variable

GPG_PASSPHRASE - Passphrase for GPG key used to sign release

EXAMPLES

release-build.sh --release-prepare --releaseVersion="0.6.0" --developmentVersion="0.7.0.dev0"
release-build.sh --release-prepare --releaseVersion="0.6.0" --developmentVersion="0.7.0.dev0" --releaseRc="rc1" --tag="v0.6.0-incubating-rc1"
release-build.sh --release-prepare --releaseVersion="0.6.0" --developmentVersion="0.7.0.dev0" --releaseRc="rc1" --dryRun

release-build.sh --release-publish --releaseVersion="0.6.0"
release-build.sh --release-publish --releaseVersion="0.6.0" --releaseRc="rc1"

EOF
  exit 1
}

set -e

if [ $# -eq 0 ]; then
  exit_with_usage
fi


# Process each provided argument configuration
while [ "${1+defined}" ]; do
  IFS="=" read -ra PARTS <<< "$1"
  case "${PARTS[0]}" in
    --release-prepare)
      GOAL="release-prepare"
      RELEASE_PREPARE=true
      shift
      ;;
    --release-publish)
      GOAL="release-publish"
      RELEASE_PUBLISH=true
      shift
      ;;
    --release-snapshot)
      GOAL="release-snapshot"
      RELEASE_SNAPSHOT=true
      shift
      ;;
    --gitCommitHash)
      GIT_REF="${PARTS[1]}"
      shift
      ;;
    --gitTag)
      GIT_TAG="${PARTS[1]}"
      shift
      ;;
    --releaseVersion)
      RELEASE_VERSION="${PARTS[1]}"
      FULL_RELEASE_VERSION="${PARTS[1]}-incubating"
      shift
      ;;
    --developmentVersion)
      DEVELOPMENT_VERSION="${PARTS[1]}"
      shift
      ;;
    --releaseRc)
      RELEASE_RC="${PARTS[1]}"
      shift
      ;;
    --tag)
      RELEASE_TAG="${PARTS[1]}"
      shift
      ;;
    --dryRun)
      DRY_RUN="-DdryRun=true"
      shift
      ;;

    *help* | -h)
      exit_with_usage
     exit 0
     ;;
    -*)
     echo "Error: Unknown option: $1" >&2
     exit 1
     ;;
    *)  # No more options
     break
     ;;
  esac
done


if [[ -z "$GPG_PASSPHRASE" ]]; then
    echo 'The environment variable GPG_PASSPHRASE is not set. Enter the passphrase to'
    echo 'unlock the GPG signing key that will be used to sign the release!'
    echo
    stty -echo && printf "GPG passphrase: " && read GPG_PASSPHRASE && printf '\n' && stty echo
fi

# --release-prepare validation
if [[ "$RELEASE_PREPARE" == "true" && -z "$RELEASE_VERSION" ]]; then
    echo "ERROR: --releaseVersion must be passed as an argument to run this script"
    exit_with_usage
fi

if [[ "$RELEASE_PREPARE" == "true" && -z "$DEVELOPMENT_VERSION" ]]; then
    echo "ERROR: --developmentVersion must be passed as an argument to run this script"
    exit_with_usage
fi

# --release-publish validation
if [[ "$RELEASE_PUBLISH" == "true" && -z "$RELEASE_VERSION" ]]; then
    echo "ERROR: --releaseVersion must be passed as an argument to run this script"
    exit_with_usage
fi

if [[ "$RELEASE_PUBLISH" == "true" && "$DRY_RUN" ]]; then
    echo "ERROR: --dryRun not supported for --release-publish"
    exit_with_usage
fi

# Commit ref to checkout when building
GIT_REF=${GIT_REF:-master}

BASE_DIR=$(pwd)

MVN="mvn"

if [ -z "$RELEASE_RC" ]; then
  RELEASE_RC="rc1"
fi

if [ -z "$RELEASE_TAG" ]; then
  RELEASE_TAG="v$FULL_RELEASE_VERSION-$RELEASE_RC"
fi

if [ -z "$RELEASE_STAGING_FOLDER" ]; then
  RELEASE_STAGING_FOLDER="$FULL_RELEASE_VERSION-$RELEASE_RC"
fi

RELEASE_STAGING_LOCATION="https://dist.apache.org/repos/dist/dev/incubator/toree/"

echo "  "
echo "-------------------------------------------------------------"
echo "------- Release configuration -------------------------------"
echo "-------------------------------------------------------------"
echo "Executing            ==> $GOAL"
echo "Git reference        ==> $GIT_REF"
echo "Release version      ==> $RELEASE_VERSION"
echo "Full Release version ==> $FULL_RELEASE_VERSION"
echo "Development version  ==> $DEVELOPMENT_VERSION"
echo "RC                   ==> $RELEASE_RC"
echo "Tag                  ==> $RELEASE_TAG"
echo "Release staging dir  ==> $RELEASE_STAGING_FOLDER"
if [ "$DRY_RUN" ]; then
   echo "dry run ?            ==> true"
fi
echo "  "
echo "Deploying to :"
echo $RELEASE_STAGING_LOCATION
echo "  "

function validate_dependency {
    if ! command -v "$1" &> /dev/null; then
        echo "ERROR: $1 is not installed or not on PATH"
        exit 1
    fi
}

function validate_prepare_dependencies {
    echo "Validating prepare dependencies..."
    validate_dependency git
    validate_dependency docker
    validate_dependency sbt
}

function validate_publish_dependencies {
    echo "Validating publish dependencies..."
    validate_dependency git
    validate_dependency svn
    validate_dependency mvn
    validate_dependency gpg
}

function validate_artifacts {
    echo "Validating release artifacts in target/toree/dist/..."
    local has_error=false

    local artifacts=(
        "target/toree/dist/apache-toree-bin/apache-toree-$FULL_RELEASE_VERSION-bin.tar.gz"
        "target/toree/dist/apache-toree-src/apache-toree-$FULL_RELEASE_VERSION-src.tar.gz"
        "target/toree/dist/toree-pip/toree-$RELEASE_VERSION.tar.gz"
        "target/toree/dist/apache-toree-pip/apache-toree-$RELEASE_VERSION.tar.gz"
        "target/toree/dist/toree/lib/toree-assembly-$FULL_RELEASE_VERSION.jar"
    )

    for artifact in "${artifacts[@]}"; do
        if [ ! -f "$BASE_DIR/$artifact" ]; then
            echo "ERROR: Missing artifact: $artifact"
            has_error=true
        fi
    done

    if [ "$has_error" = true ]; then
        echo ""
        echo "Release artifacts are incomplete. Run --release-prepare first."
        exit 1
    fi

    echo "All release artifacts found."
}

set -o xtrace

function checkout_code {
    rm -rf target
    mkdir target
    cd target
    rm -rf toree
    git clone https://gitbox.apache.org/repos/asf/incubator-toree.git toree
    cd toree
    git checkout $GIT_REF
    git_hash=`git rev-parse --short HEAD`
    echo "Checked out Incubator Toree git hash $git_hash"

    cd "$BASE_DIR" #return to base dir
}

###############################################################################
# release-prepare
#
# Prepares a release locally: clone, version bump, tag, and build.
# Does NOT push to any external system (git, SVN, Maven).
###############################################################################
if [[ "$RELEASE_PREPARE" == "true" ]]; then
    echo "Preparing release $FULL_RELEASE_VERSION ($RELEASE_VERSION)"

    validate_prepare_dependencies

    # Checkout code
    checkout_code
    cd target/toree

    # Bump version to release, commit, and tag
    sed -i .bak "s@^BASE_VERSION.*@BASE_VERSION?=$RELEASE_VERSION@g" Makefile
    git commit Makefile -m"Prepare release $FULL_RELEASE_VERSION"
    git tag $RELEASE_TAG
    git_tag_hash=`git rev-parse --short HEAD`

    # Bump version to next development iteration
    sed -i .bak "s@^BASE_VERSION.*@BASE_VERSION?=$DEVELOPMENT_VERSION@g" Makefile
    git commit Makefile -m"Prepare for next development iteration $DEVELOPMENT_VERSION"
    echo "Created release tag $RELEASE_TAG at git hash $git_tag_hash"

    cd .. #exit toree

    if [ -z "$DRY_RUN" ]; then
        cd "$BASE_DIR/target/toree"
        git checkout $RELEASE_TAG
        git clean -d -f -x

        make clean dist release
    fi

    cd "$BASE_DIR" #return to base dir

    echo ""
    echo "-------------------------------------------------------------"
    echo "------- Release preparation complete ------------------------"
    echo "-------------------------------------------------------------"
    echo ""
    echo "Release artifacts are available at: $BASE_DIR/target/toree/dist/"
    echo ""
    echo "Review the artifacts, then publish the release with:"
    echo ""
    echo "  release-build.sh --release-publish --releaseVersion=\"$RELEASE_VERSION\" --releaseRc=\"$RELEASE_RC\""
    echo ""

    exit 0
fi


###############################################################################
# release-publish
#
# Publishes a previously prepared release. Pushes git commits and tags,
# stages artifacts to Apache SVN dist/dev, and deploys the assembly jar
# to the Apache Maven staging repository.
#
# Requires --release-prepare to have been run first.
###############################################################################
if [[ "$RELEASE_PUBLISH" == "true" ]]; then
    echo "Publishing release $FULL_RELEASE_VERSION ($RELEASE_VERSION)"

    validate_publish_dependencies

    # Verify that prepare was run and artifacts exist
    if [ ! -d "$BASE_DIR/target/toree" ]; then
        echo "ERROR: target/toree does not exist. Run --release-prepare first."
        exit 1
    fi

    validate_artifacts

    # Push release commits and tag to remote
    cd "$BASE_DIR/target/toree"
    git checkout master
    git push
    git push --tags

    # Stage artifacts in Apache SVN dist/dev
    cd "$BASE_DIR/target"
    svn co $RELEASE_STAGING_LOCATION svn-toree
    mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/toree
    mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/toree-pip
    mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip

    cp toree/dist/apache-toree-bin/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree
    cp toree/dist/apache-toree-src/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree
    cp toree/dist/toree-pip/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree-pip
    cp -r toree/dist/toree-pip/toree.egg-info svn-toree/$RELEASE_STAGING_FOLDER/toree-pip/
    cp toree/dist/apache-toree-pip/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip
    cp -r toree/dist/apache-toree-pip/*toree.egg-info svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip/

    # GPG sign and generate checksums for SVN staging
    cd "$BASE_DIR/target/svn-toree/$RELEASE_STAGING_FOLDER/toree"
    rm -f *.asc
    for i in *.tar.gz; do gpg --output $i.asc --detach-sig --armor $i; done
    rm -f *.sha*
    for i in *.tar.gz; do shasum --algorithm 512 $i > $i.sha512; done

    cd "$BASE_DIR/target/svn-toree/$RELEASE_STAGING_FOLDER/toree-pip"
    rm -f *.asc
    for i in *.tar.gz; do gpg --output $i.asc --detach-sig --armor $i; done
    rm -f *.sha*
    for i in *.tar.gz; do shasum --algorithm 512 $i > $i.sha512; done

    cd "$BASE_DIR/target/svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip"
    rm -f *.asc
    for i in *.tar.gz; do gpg --output $i.asc --detach-sig --armor $i; done
    rm -f *.sha*
    for i in *.tar.gz; do shasum --algorithm 512 $i > $i.sha512; done

    # Commit staged artifacts to SVN
    cd "$BASE_DIR/target/svn-toree/"
    svn add $RELEASE_STAGING_FOLDER/
    svn ci -m"Apache Toree $RELEASE_STAGING_FOLDER"

    # Deploy assembly jar to Apache Maven staging
    cd "$BASE_DIR/target"
    mvn gpg:sign-and-deploy-file -DgroupId=org.apache.toree -DartifactId=toree-assembly -Dversion=$RELEASE_VERSION-incubating -Dpackaging=jar -Dfile=toree/dist/toree/lib/toree-assembly-$RELEASE_VERSION-incubating.jar -DrepositoryId=apache.releases.https -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -Dpassphrase=$GPG_PASSPHRASE

    cd "$BASE_DIR" #return to base dir

    echo ""
    echo "-------------------------------------------------------------"
    echo "------- Release publish complete ----------------------------"
    echo "-------------------------------------------------------------"
    echo ""
    echo "Git commits and tag pushed to remote"
    echo "Artifacts staged to $RELEASE_STAGING_LOCATION$RELEASE_STAGING_FOLDER/"
    echo "Assembly jar deployed to Apache Maven staging"
    echo ""

    exit 0
fi

cd "$BASE_DIR" #return to base dir
rm -rf target
echo "ERROR: wrong execution goals"
exit_with_usage
