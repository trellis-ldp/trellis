#!/bin/bash

VERSION=$(./gradlew -q getVersion)

# Publish releases only
if [[ $VERSION != *SNAPSHOT* ]]; then

    cd platform/quarkus
    ##################################
    # Quarkus-based triplestore image
    ##################################
    IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-triplestore

    ../../gradlew assemble -Ptriplestore

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"

    ###############################
    # Quarkus-based database image
    ###############################
    IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-postgresql

    ../../gradlew assemble

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"

    #########################
    # Dropwizard-based image
    #########################
    IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis

    cd ../dropwizard
    ../../gradlew assemble

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
fi

