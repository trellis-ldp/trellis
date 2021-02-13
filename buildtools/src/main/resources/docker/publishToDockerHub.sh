#!/bin/bash

VERSION=$(./gradlew -q getVersion)
BRANCH=$(git branch 2>/dev/null | sed -n -e 's/^\* \(.*\)/\1/p')
cd platform/quarkus

##################################
# Quarkus-based triplestore image
##################################
IMAGE=trellisldp/trellis-triplestore

../../gradlew assemble -Ptriplestore

TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT* ]]; then
    TAG=develop
fi

# Don't use latest/develop tags for maintenance branches
if [[ $BRANCH == *.x ]]; then
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
else
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$TAG"
    docker push "$IMAGE:$VERSION"
fi


###############################
# Quarkus-based database image
###############################
IMAGE=trellisldp/trellis-postgresql

../../gradlew assemble

TAG=latest
# Use the develop tag for snapshots
if [[ $VERSION == *SNAPSHOT* ]]; then
    TAG=develop
fi

# Don't use latest/develop tags for maintenance branches
if [[ $BRANCH == *.x ]]; then
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$VERSION"
else
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE:$TAG" -t "$IMAGE:$VERSION" .
    docker push "$IMAGE:$TAG"
    docker push "$IMAGE:$VERSION"
fi


