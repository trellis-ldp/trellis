# trellis-audit

A default implementation of an audit service for Trellis.
This module will generate audit-related data for
standard create/update/delete operations using the PROV vocabulary.

An audit service is optional in the HTTP layer, but if present,
a client can retrieve these data with a Prefer header such as:

```http
Prefer: return=representation; include="http://www.trellisldp.org/ns/trellis#PreferAudit"
```

## Create events

A sample `create` event will follow this structure:

```turtle
<https://example.com/resource>
        prov:wasGeneratedBy  _:b0 .

_:b0    rdf:type                prov:Activity ;
        rdf:type                as:Create ;
        prov:wasAssociatedWith  <https://example.com/user/1> ;
        prov:atTime      "2017-10-23T19:48:16.076Z"^^xsd:dateTime .
```


## Update events

A sample `update` event will follow this structure:

```turtle
<https://example.com/resource>
        prov:wasGeneratedBy  _:b1 .

_:b1    rdf:type                prov:Activity ;
        rdf:type                as:Update ;
        prov:wasAssociatedWith  <https://example.com/user/1> ;
        prov:atTime      "2017-10-23T19:48:16.076Z"^^xsd:dateTime .
```


## Delete events

A sample `delete` event will follow this structure:

```turtle
<https://example.com/resource>
        prov:wasGeneratedBy  _:b2 .

_:b2    rdf:type                prov:Activity ;
        rdf:type                as:Delete ;
        prov:wasAssociatedWith  <https://example.com/user/1> ;
        prov:atTime      "2017-10-23T19:48:16.076Z"^^xsd:dateTime .
```

