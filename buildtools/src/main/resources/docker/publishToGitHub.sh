#!/bin/bash

IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-triplestore

VERSION=$(./gradlew -q getVersion)

# Publish releases only
if [[ $VERSION != *SNAPSHOT* ]]; then
    cd platform/quarkus
    ../../gradlew assemble

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
fi

