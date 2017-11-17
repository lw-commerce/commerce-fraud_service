#!/bin/bash

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
if [ -z $GO_PIPELINE_LABEL ]; then
  export BUILD_NUMBER=000
else
  export BUILD_NUMBER=$GO_PIPELINE_LABEL
fi

export BUILD_VERSION=1.0.${BUILD_NUMBER}

source $PROJECT_DIR/pipeline/build.env

DOCKER_LABEL=${PROJECT_NAME}.${BUILD_NUMBER}

main() {
  test_in_docker
  build

  if [ "$1" = "no-docker" ]; then
    exit
  fi

  docker_create_image
  write_dockerimage_hash
}

test_in_docker() {

  mkdir -p $PROJECT_DIR/export
  mkdir -p $PROJECT_DIR/build
  rm -rf $PROJECT_DIR/export/*

  trap "trap - EXIT && error_exit 'exiting unexpectedly...'" SIGINT SIGTERM EXIT

  docker run -t --rm \
    -v $PROJECT_DIR:/opt/build \
    --add-host=artifactory.lifeway.org:172.16.41.36 \
    --env-file $PROJECT_DIR/pipeline/build.env \
    --env=BUILD_VERSION \
    --label $DOCKER_LABEL \
    natewarr/java-docker:1.3 \
    ./activator -Duser.home=/opt/build clean compile coverage test
  [ "$?" -eq "0" ] || error_exit "Tests failed."

  trap - EXIT SIGINT SIGTERM


  cp -r $PROJECT_DIR/target/scala-*/scoverage-report $PROJECT_DIR/build/scoverage-report
  cp -r $PROJECT_DIR/target/scala-*/coverage-report $PROJECT_DIR/build/coverage-report
  cp -r $PROJECT_DIR/target/test-reports $PROJECT_DIR/build/test-reports

}

build() {
  trap "trap - EXIT && error_exit 'exiting unexpectedly...'" SIGINT SIGTERM EXIT

  docker run -t --rm \
    -v $PROJECT_DIR:/opt/build \
    --add-host=artifactory.lifeway.org:172.16.41.36 \
    --env-file $PROJECT_DIR/pipeline/build.env \
    --env=BUILD_VERSION \
    --label $DOCKER_LABEL \
    natewarr/java-docker:1.3 \
    ./activator -Duser.home=/opt/build universal:package-zip-tarball
  [ "$?" -eq "0" ] || error_exit "Build failed."

  trap - EXIT SIGINT SIGTERM
}

docker_create_image() {
  docker build -t $DOCKER_IMAGE_NAME:1.0.$BUILD_NUMBER $PROJECT_DIR || error_exit "Failed to build docker image"
}

write_dockerimage_hash() {
  echo "Write docker image hash to a file"
  docker images -q $DOCKER_IMAGE_NAME:1.0.$BUILD_NUMBER > $PROJECT_DIR/build/dockerimage.txt
  cat $PROJECT_DIR/build/dockerimage.txt
}

error_exit() {
  trap - EXIT SIGINT SIGTERM
  echo $1

  echo "Deleting any leftover docker containers..."
  if (>/dev/null docker ps --filter label=$DOCKER_LABEL -q); then
    docker rm --force $(docker ps -q --filter label=$DOCKER_LABEL)
  fi

  exit 1
}

main $@
