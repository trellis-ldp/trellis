# trellis-karaf

This module provides a `features` repository for [OSGi provisioning](https://karaf.apache.org/manual/latest/provisioning)
in [Apache Karaf](https://karaf.apache.org).

In Karaf, these features can be installed after first provisioning a concrete RDF implementation (e.g. Jena).
Note: the version numbers below are included for reference. A user should generally use the latest released version of the components.

```
feature:repo-add mvn:org.apache.jena/jena-osgi-features/3.6.0/xml/features
feature:repo-add mvn:org.trellisldp/trellis-karaf/0.5.1/xml/features
feature:install commons-rdf-jena
```

Now, the desired Trellis features can be installed, as desired.

```
feature:install trellis-api trellis-http trellis-audit trellis-constraint-rules
```


