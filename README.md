# Trellis Linked Data Server

A scalable platform for building [linked data](https://www.w3.org/TR/ldp/) applications.

[![Build Status](https://travis-ci.org/trellis-ldp/trellis.png?branch=master)](https://travis-ci.org/trellis-ldp/trellis)
[![Build status](https://ci.appveyor.com/api/projects/status/nvdwx442663ib39d?svg=true)](https://ci.appveyor.com/project/acoburn/trellis)
[![Coverage Status](https://coveralls.io/repos/github/trellis-ldp/trellis/badge.svg?branch=master)](https://coveralls.io/github/trellis-ldp/trellis?branch=master)

## Mailing list

A [mailing list](https://groups.google.com/group/trellis-ldp) is available to anyone who is interested in using, contributing to or simply learning more about Trellis.

## Documentation

General documentation for Trellis is available on the [project wiki](https://github.com/trellis-ldp/trellis/wiki).

Javadocs for each project is available at https://trellis-ldp.github.io/trellis/apidocs/

## Features

Trellis has been designed with four primary goals:

  * Scalability
  * Long-term durability of content
  * Modularity
  * Adherence to existing web standards

### Scalability

Trellis has been built on components that already support horizontal scalability: Kafka, Zookeeper and Spark. While it is
possible to run Trellis on a single machine, scaling out across a cluster is well-defined and supported. Trellis is
"eventually consistent", meaning that many operations run asynchronously. While this makes the system very responsive, it
also means that clients cannot expect operations to be atomic. In general, per-resource operations are atomic; operations
that cause other resources to change are handled asynchronously.

### Durability

Data integrity is vitally important for content that is stored for years and decades. Trellis makes it possible to retrieve
the state of any resource at any, arbitrary point in time. This also means that nothing is ever truly deleted (though if you
need to purge a resource from the persistence layer, there is a mechanism for that). For every operation that changes a
resource, there is a full audit log available through standard LDP mechanisms. This audit log lists who made what change and
when that change was made.

### Modularity

The overall code base for Trellis is small, and it is divided up into even smaller modules each of which can be maintained
independently. This simplifies maintenance and it also makes it easy to customize individual components as needed. Trellis
has also been designed to fully support OSGi deployment (scheduled for the 0.2.0 release).

### Flexibility

Because Trellis is built on top of LDP, clients that interact with it tend to use a lot of RDF (e.g. JSON-LD). Trellis
enforces only a very minimal set of restrictions on what RDF is allowable: basically, if LDP prohibits it, Trellis does not
allow it, but otherwise, pretty much anything goes. You can use any RDF vocabulary; you can store binaries of any type. Any
special handling of particular content types needs to be handled in another layer of your software stack.

### Web Standards

There are a lot of standards in existence. Trellis has selected to conform to a collection of well-defined and broadly used
specifications because doing so provides a solid and well-understood foundation for interacting with the software. This also
makes the Trellis API stable and consistent.

### External Integrations

Any time a resource is created, modified or deleted, a notification is made available, making it easy to use an integration
framework to connect Trellis to external applications. The notifications provide enough information to make informed routing
decisions without being too heavy.

## Underlying specifications

Trellis is an [HTTP/1.1](https://tools.ietf.org/html/rfc7231) server, which complies with the following specifications:

  * [W3C LDP Server](https://www.w3.org/TR/ldp/)
  * [W3C Activity Streams 2.0](https://www.w3.org/TR/activitystreams-core/)
  * [Solid WebAC](https://github.com/solid/solid-spec#authorization-and-access-control) (Authorization and Access Control)
  * [RFC 7089](https://tools.ietf.org/html/rfc7089) (HTTP Framework for Time-Based Access to Resource States -- Memento)
  * [RFC 3230](https://tools.ietf.org/html/rfc3230) (Instance Digests in HTTP)

## Source Code

All source code is open source and licensed as Apache 2. Contributions are welcome

### Trellis Vocabulary

This [vocabulary](https://github.com/trellis-ldp/trellis-ontology) defines the Trellis-specific RDF terms.

### Implementations

This repository contains only the core abstractions and interfaces for Trellis along with a number of default service
implementations. It does not contain an implementation for a persistence layer; however, below is a list of known
implementations.

 * [Rosid](https://github.com/trellis-ldp/trellis-rosid): A Kafka-based application that uses file-based persistence.

## Main dependencies

  * [Commons-RDF](https://commons.apache.org/proper/commons-rdf/) -- for RDF abstractions
  * [Jena](https://jena.apache.org/) -- for RDF I/O processing
  * [Jackson](https://github.com/FasterXML/jackson) -- for JSON processing
  * [Jersey](https://jersey.java.net/) -- for HTTP processing

## Related projects

  * [py-ldnlib](https://github.com/trellis-ldp/py-ldnlib) A Python3 library for linked data notifications
  * [static-ldp](https://github.com/trellis-ldp/static-ldp) A PHP application that serves static files as LDP resources

## Building

 * All projects are written in Java and require at least Java 8. All projects can be built with Gradle.

    ./gradlew install

