# trellis-constraint-rules

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.trellisldp/trellis-constraint-rules/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.trellisldp/trellis-constraint-rules/)

A set of constraints on a Trellis server defining the rules that govern valid RDF.

These rules consist of:

  * No inappropriate LDP properties -- certain LDP properties can only be modified if the interaction model permits it
  * LDP Resource types cannot be set or changed by changing RDF triples
  * Certain properties (`acl:accessControl`, `ldp:membershipResource`) must be used with "in-domain" resources
  * Certain properties must have a range of a IRI and have a max-cardinality of 1 (`ldp:membershipResource`, `ldp:hasMemberRelation`, `ldp:isMemberOfRelation`, `ldp:insertedContentRelation`, `ldp:inbox`, `acl:accessControl`, `oa:annotationService`)

## Building

This code requires Java 8 and can be built with Gradle:

    ./gradlew install
