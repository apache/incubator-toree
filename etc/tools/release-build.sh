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

usage: release-build.sh [--release-prepare | --release-publish | --release-snapshot]

DESCRIPTION

Use maven infrastructure to create a project release package and publish
to staging release location (https://dist.apache.org/repos/dist/dev/incubator/toree/)
and maven staging release repository.

--release-prepare --releaseVersion="0.2.0" --developmentVersion="0.2.0.dev1" [--releaseRc="rc1"] [--tag="v2.0.0"] [--gitCommitHash="a874b73"]
This form execute maven release:prepare and upload the release candidate distribution
to the staging release location.

--release-publish --gitCommitHash="a874b73"
Publish the maven artifacts of a release to the Apache staging maven repository.

OPTIONS

--releaseVersion     - Release identifier used when publishing
--developmentVersion - Release identifier used for next development cyce
--releaseRc          - Release RC identifier used when publishing, default 'rc1'
--tag                - Release Tag identifier used when taging the release, default 'v$releaseVersion'
--gitCommitHash      - Release tag or commit to build from, default master HEAD
--dryRun             - Dry run only, mostly used for testing.

A GPG passphrase is expected as an environment variable

GPG_PASSPHRASE - Passphrase for GPG key used to sign release

EXAMPLES

release-build.sh --release-prepare --releaseVersion="0.2.0" --developmentVersion="0.3.0.dev1"
release-build.sh --release-prepare --releaseVersion="0.2.0" --developmentVersion="0.3.0.dev1" --releaseRc="rc1" --tag="v0.2.0-incubating-rc1"
release-build.sh --release-prepare --releaseVersion="0.2.0" --developmentVersion="0.3.0.dev1" --releaseRc="rc1" --tag="v0.2.0-incubating-rc1"  --gitCommitHash="a874b73" --dryRun

release-build.sh --release-publish --gitCommitHash="a874b73"
release-build.sh --release-publish --gitTag="v0.2.0-incubating-rc1"

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

if [[ "$RELEASE_PREPARE" == "true" && -z "$RELEASE_VERSION" ]]; then
    echo "ERROR: --releaseVersion must be passed as an argument to run this script"
    exit_with_usage
fi

if [[ "$RELEASE_PREPARE" == "true" && -z "$DEVELOPMENT_VERSION" ]]; then
    echo "ERROR: --developmentVersion must be passed as an argument to run this script"
    exit_with_usage
fi

if [[ "$RELEASE_PUBLISH" == "true"  ]]; then
    if [[ "$GIT_REF" && "$GIT_TAG" ]]; then
        echo "ERROR: Only one argumented permitted when publishing : --gitCommitHash or --gitTag"
        exit_with_usage
    fi
    if [[ -z "$GIT_REF" && -z "$GIT_TAG" ]]; then
        echo "ERROR: --gitCommitHash OR --gitTag must be passed as an argument to run this script"
        exit_with_usage
    fi
fi

if [[ "$RELEASE_PUBLISH" == "true" && "$DRY_RUN" ]]; then
    echo "ERROR: --dryRun not supported for --release-publish"
    exit_with_usage
fi

# Commit ref to checkout when building
GIT_REF=${GIT_REF:-master}
if [[ "$RELEASE_PUBLISH" == "true" && "$GIT_TAG" ]]; then
    GIT_REF="tags/$GIT_TAG"
fi

BASE_DIR=$(pwd)

MVN="mvn"
PUBLISH_PROFILES="-Pdistribution"

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
echo "------- Release preparation with the following parameters ---"
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
   echo "dry run ?           ==> true"
fi
echo "  "
echo "Deploying to :"
echo $RELEASE_STAGING_LOCATION
echo "  "

set -o xtrace

function checkout_code {
    # Checkout code
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

if [[ "$RELEASE_PREPARE" == "true" ]]; then
    echo "Preparing release $FULL_RELEASE_VERSION ($RELEASE_VERSION)"
    # Checkout code
    checkout_code
    cd target/toree

    # Build and prepare the release
    sed -i .bak "s@^BASE_VERSION.*@BASE_VERSION?=$RELEASE_VERSION@g" Makefile
    git commit Makefile -m"Prepare release $FULL_RELEASE_VERSION"
    git tag $RELEASE_TAG
    git_tag_hash=`git rev-parse --short HEAD`
    sed -i .bak "s@^BASE_VERSION.*@BASE_VERSION?=$DEVELOPMENT_VERSION@g" Makefile
    git commit Makefile -m"Prepare for next development interaction $DEVELOPMENT_VERSION"
    git push
    git push --tags

    cd .. #exit toree

    if [ -z "$DRY_RUN" ]; then
        cd "$BASE_DIR/target/toree"
        git checkout $RELEASE_TAG
        git clean -d -f -x

        make clean dist release

        cd "$BASE_DIR/target"
        svn co $RELEASE_STAGING_LOCATION svn-toree
        mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/toree
        mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/toree-pip
        mkdir -p svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip

        cp toree/dist/toree-bin/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree
        cp toree/dist/toree-src/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree
        cp toree/dist/toree-pip/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/toree-pip
        cp -r toree/dist/toree-pip/toree.egg-info svn-toree/$RELEASE_STAGING_FOLDER/toree-pip/
        cp toree/dist/apache-toree-pip/*.tar.gz svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip
        cp -r toree/dist/apache-toree-pip/*toree.egg-info svn-toree/$RELEASE_STAGING_FOLDER/apache-toree-pip/

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

        cd "$BASE_DIR/target/svn-toree/" #exit $RELEASE_STAGING_FOLDER/

        svn add $RELEASE_STAGING_FOLDER/
        svn ci -m"Apache Toree $RELEASE_STAGING_FOLDER"

        cd "$BASE_DIR/target"
        mvn gpg:sign-and-deploy-file -DgroupId=org.apache.toree -DartifactId=toree-assembly -Dversion=$RELEASE_VERSION-incubating -Dpackaging=jar -Dfile=toree/dist/toree/lib/toree-assembly-$RELEASE_VERSION-incubating.jar -DrepositoryId=apache.releases.https -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -Dpassphrase=$GPG_PASSPHRASE
    fi

    cd "$BASE_DIR" #exit target

    exit 0
fi


if [[ "$RELEASE_PUBLISH" == "true" ]]; then
    echo "Preparing release $RELEASE_VERSION"
    # Checkout code
    cd "$BASE_DIR/target/toree"
    git checkout $RELEASE_TAG
    git clean -d -f -x

    make clean dist release

    cd "$BASE_DIR/target"
    mvn gpg:sign-and-deploy-file -DgroupId=org.apache.toree -DartifactId=toree-assembly -Dversion=$RELEASE_VERSION-incubating -Dpackaging=jar -Dfile=toree/dist/toree/lib/toree-assembly-$RELEASE_VERSION-incubating.jar -DrepositoryId=apache.releases.https -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -Dpassphrase=$GPG_PASSPHRASE

    cd "$BASE_DIR" #exit target

    exit 0
fi

cd "$BASE_DIR" #return to base dir
rm -rf target
echo "ERROR: wrong execution goals"
exit_with_usage
