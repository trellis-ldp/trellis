# Trellis Linked Data Server

A scalable platform for building [linked data](https://www.w3.org/TR/ldp/) applications.

![Build Status](https://github.com/trellis-ldp/trellis/workflows/GitHub%20CI/badge.svg)
[![Coverage Status](https://coveralls.io/repos/github/trellis-ldp/trellis/badge.svg?branch=master)](https://coveralls.io/github/trellis-ldp/trellis?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cd0f09e025ce4a9e9c00fd7b04fb66fb)](https://www.codacy.com/gh/trellis-ldp/trellis?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=trellis-ldp/trellis&amp;utm_campaign=Badge_Grade)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/trellis-ldp/trellis.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/trellis-ldp/trellis/alerts/)
![Maven Central](https://img.shields.io/maven-central/v/org.trellisldp/trellis-api.svg)

Trellis is a rock-solid, enterprise-ready linked data server.
The quickest way to get started with Trellis is to either use
a pre-built [docker container](https://hub.docker.com/r/trellisldp/trellis) or download the
[latest release](https://www.trellisldp.org/download.html).

Trellis is built on existing [Web standards](https://github.com/trellis-ldp/trellis/wiki/Web-Standards).
It is modular, extensible and fast.

* [Wiki](https://github.com/trellis-ldp/trellis/wiki)
* [Configuration guide](https://github.com/trellis-ldp/trellis/wiki/Configuration-Guide)
* [Mailing List](https://groups.google.com/group/trellis-ldp)
* [API Documentation](https://www.trellisldp.org/docs/trellis/current/apidocs/) (JavaDocs)
* [Website](https://www.trellisldp.org)

All source code is open source and licensed as Apache 2. Contributions are always welcome.

## Docker Containers

Docker containers for Trellis are published on [Docker Hub](https://hub.docker.com/u/trellisldp).
Container environments are published with every commit to `master` and are available for all stable
releases. More details are available on the
[Trellis Wiki](https://github.com/trellis-ldp/trellis/wiki/Dockerized-Trellis).

Docker pull command

```bash
docker pull trellisldp/trellis-triplestore
```

Or, for the PostgreSQL-based persistence layer

```bash
docker pull trellisldp/trellis-postgresql
```

## Building Trellis

In most cases, you won't need to compile Trellis. Released components are available on Maven Central,
and the deployable application can be [downloaded](https://www.trellisldp.org/download.html) directly
from the Trellis website. However, if you want to build the latest snapshot, you will need, at the very least,
to have Java 8+ available. The software can be built with [Gradle](https://gradle.org) using this command:

```bash
./gradlew install
```

## Related projects

* [py-ldnlib](https://github.com/trellis-ldp/py-ldnlib) A Python3 library for linked data notifications
* [static-ldp](https://github.com/trellis-ldp/static-ldp) A PHP application that serves static files as LDP resources
* [trellis-extensions](https://github.com/trellis-ldp/trellis-extensions) Trellis extension modules
* [camel-ldp-recipes](https://github.com/trellis-ldp/camel-ldp-recipes) Integration workflows built with [Apache Camel](https://camel.apache.org)

