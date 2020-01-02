#!/bin/bash

IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-triplestore

VERSION=$(./gradlew -q getVersion)
BRANCH=$(git branch 2>/dev/null | sed -n -e 's/^\* \(.*\)/\1/p')

cd platform/quarkus
../../gradlew assemble

TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT* ]]; then
    TAG=develop
fi

# Don't use latest/develop tags for maintenance branches
if [[ $BRANCH == *.x ]]; then
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
else
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
fi

docker push $IMAGE
