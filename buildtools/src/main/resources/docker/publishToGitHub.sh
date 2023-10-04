#!/bin/bash

VERSION=$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)

# Publish releases only
if [[ $VERSION != *SNAPSHOT* ]]; then

    ./mvnw package -pl apps/quarkus -am

    cd apps/quarkus
    ##################################
    # Quarkus-based triplestore image
    ##################################
    IMAGE=docker.pkg.github.com/trellis-ldp/trellis/trellis-triplestore

    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
fi

