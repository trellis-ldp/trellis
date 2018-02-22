#!/bin/bash

if [ "$JDK_RELEASE" != "JDK 10" ]; then
    ./gradlew jacocoRootReport coveralls sonarqube
fi
