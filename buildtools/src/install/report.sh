#!/bin/bash

if [ "$JDK_RELEASE" != "JDK 10 Early-Access" ]; then
    ./gradlew jacocoRootReport coveralls sonarqube
fi
