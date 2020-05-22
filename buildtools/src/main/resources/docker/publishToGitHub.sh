#!/bin/bash

VERSION=$(./gradlew -q getVersion)
cd platform/quarkus

##################################
# Quarkus-based triplestore image
##################################
IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-triplestore

# Publish releases only
if [[ $VERSION != *SNAPSHOT* ]]; then
    ../../gradlew assemble -Ptriplestore

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
fi

###############################
# Quarkus-based database image
###############################
IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-postgresql

# Publish releases only
if [[ $VERSION != *SNAPSHOT* ]]; then
    ../../gradlew assemble

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
fi

