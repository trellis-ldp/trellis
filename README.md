# Trellis Linked Data Server

A scalable platform for building [linked data](https://www.w3.org/TR/ldp/) applications.

[![Build Status](https://travis-ci.org/trellis-ldp/trellis.png?branch=master)](https://travis-ci.org/trellis-ldp/trellis)
[![Coverage Status](https://coveralls.io/repos/github/trellis-ldp/trellis/badge.svg?branch=master)](https://coveralls.io/github/trellis-ldp/trellis?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/09f8d4ae61764bd9a1fead16514b6db2)](https://www.codacy.com/app/acoburn/trellis?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=trellis-ldp/trellis&amp;utm_campaign=Badge_Grade)
![Maven Central](https://img.shields.io/maven-central/v/org.trellisldp/trellis-api.svg)

General documentation for [Trellis](https://www.trellisldp.org) is available on the [project wiki](https://github.com/trellis-ldp/trellis/wiki).
[API (JavaDocs) documentation](https://trellis-ldp.github.io/trellis/apidocs) is also available for the latest release.

## Mailing list

A [mailing list](https://groups.google.com/group/trellis-ldp) is available to anyone who is interested in using, contributing to or simply learning more about Trellis.

## Downloads

The fastest way to get started with Trellis is to download the [latest release](https://github.com/trellis-ldp/trellis/releases)
and follow these [installation instructions](platform/linux).

## Features

Trellis has been designed with four primary goals:

  * Scalability
  * Long-term durability of content
  * Modularity
  * Adherence to existing web standards

### Scalability

Trellis has been built from the ground up to support horizontal scalability. While it is
possible to run Trellis on a single machine, scaling out across a cluster is well-defined and supported. Trellis is
eventually consistent, which is to say that many operations run asynchronously. While this makes the system very responsive, it
also means that clients cannot always expect operations to be atomic. In general, per-resource operations are atomic; operations
that cause other resources to change tend to be handled asynchronously.

### Durability

Data integrity is vitally important for content that is stored for years and decades. Trellis makes it possible to retrieve
the state of any resource at any, arbitrary point in time. This also means that nothing is ever truly deleted. For every
operation that changes a resource, there is a full audit log available through standard LDP mechanisms. This audit log
lists who made what change and when that change was made.

### Modularity

The overall code base for Trellis is small, and it is divided up into even smaller modules.
This simplifies maintenance and it also makes it easy to customize individual components as needed. Trellis
has also been designed to fully support OSGi deployment.

### Web Standards

The Trellis project has selected to conform to a collection of well-defined and broadly used specifications because
doing so provides a solid and well-understood foundation for interacting with the software. This also makes the
Trellis API stable and consistent.

### Flexibility

Because Trellis is built on top of LDP, clients that interact with it tend to use a lot of RDF (e.g. JSON-LD). Trellis
enforces only a very minimal set of restrictions on what RDF is allowable: if LDP prohibits it, Trellis does not
allow it, but otherwise, pretty much anything goes. You can use any RDF vocabulary; you can store binaries of any type.

### External Integrations

Any time a resource is created, modified or deleted, a notification is made available, making it easy to use an integration
framework to connect Trellis to external applications. The notifications provide enough information to make informed routing
decisions.

## Underlying specifications

Trellis is an [HTTP/1.1](https://tools.ietf.org/html/rfc7231) server, which complies with the following specifications:

  * [W3C LDP Server](https://www.w3.org/TR/ldp/)
  * [W3C Activity Streams 2.0](https://www.w3.org/TR/activitystreams-core/)
  * [W3C WebSub Publisher](https://www.w3.org/TR/websub/)
  * [Solid WebAC](https://github.com/solid/solid-spec#authorization-and-access-control) (Authorization and Access Control)
  * [RFC 7089](https://tools.ietf.org/html/rfc7089) (HTTP Framework for Time-Based Access to Resource States -- Memento)
  * [RFC 3230](https://tools.ietf.org/html/rfc3230) (Instance Digests in HTTP)

## Source Code

All source code is open source and licensed as Apache 2. Contributions are welcome.

### Trellis Vocabulary

This [vocabulary](https://github.com/trellis-ldp/trellis-ontology) defines the Trellis-specific
[RDF terms](https://www.trellisldp.org/ns/trellis.html).

### Implementations

This repository contains the core abstractions and interfaces for Trellis along with a number of default service
implementations. It contains a [reference implementation](components/app-triplestore) that uses a [persistence layer](components/triplestore)
based on a triplestore or local dataset. In addition, below is a list of other known implementations.

 * [Rosid](https://github.com/trellis-ldp/trellis-rosid): A Kafka-based application that uses file-based persistence.

## Principle dependencies

  * [Commons-RDF](https://commons.apache.org/proper/commons-rdf/) -- for RDF abstractions
  * [Jena](https://jena.apache.org/) -- for RDF I/O processing
  * [Jackson](https://github.com/FasterXML/jackson) -- for JSON processing

## Related projects

  * [py-ldnlib](https://github.com/trellis-ldp/py-ldnlib) A Python3 library for linked data notifications
  * [static-ldp](https://github.com/trellis-ldp/static-ldp) A PHP application that serves static files as LDP resources

## Building

 * Trellis is written in Java and requires at least Java 8. It can be built with [Gradle](https://gradle.org).

```
$ ./gradlew install
````

