#!/bin/bash

VERSION=$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)
BRANCH=$(git branch 2>/dev/null | sed -n -e 's/^\* \(.*\)/\1/p')

##################################
# Quarkus-based triplestore image
##################################
IMAGE=trellisldp/trellis-triplestore

./mvnw package -pl platform/quarkus -am

TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT* ]]; then
    TAG=develop
fi

cd platform/quarkus
# Don't use latest/develop tags for maintenance branches
if [[ $BRANCH == *.x ]]; then
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
else
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$TAG"
    docker push "$IMAGE:$VERSION"
fi

