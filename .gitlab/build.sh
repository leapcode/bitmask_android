#!/bin/bash
set -x
#
# Build script for the gitlab ci.
#
# usage: build.sh DIR [TAG]
#
# Will run docker build in DIR with DIR/Dockerfile
#
# Assumes CI specific environment variables to be set:
# CI_REGISTRY_IMAGE
#

DIR=$1
TAG=${2:-latest}
DOCKERFILE=docker/${DIR}/${2:-Dockerfile}${2:+.dockerfile}
TARGET=${CI_REGISTRY_IMAGE}/${DIR}:${TAG}

function quit {
    echo "Image build failed. Exit value: $?."
    exit 1
}

if git show HEAD~1 --name-only | grep "$DOCKERFILE"; then
  docker login -u gitlab-ci-token -p "$CI_JOB_TOKEN" "$CI_REGISTRY" || quit
  docker build -t "$TARGET" -f "$DOCKERFILE" docker/ || quit
  docker push "$TARGET" || quit
fi
