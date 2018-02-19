# trellis-event-serialization

A serialization service for a Trellis event stream. Specifically, this converts a Trellis Event into
a serialized JSON-LD String.

For example, this implementation will serialize an update event into structures such as:

```javascript
{
  "@context": "https://www.w3.org/ns/activitystreams",
  "id": "urn:uuid:031b0857-b1bd-4f19-989d-1ab0e22ca57c",
  "type": ["Update" , "http://www.w3.org/ns/prov#Activity"],
  "actor": ["https://people.apache.org/~acoburn/#i"],
  "object": {
    "id": "http://localhost:8080/resource",
    "type": ["http://www.w3.org/ns/ldp#Container"]
  }
}
```

These notifications conform to the [W3C Activity Streams](https://www.w3.org/TR/activitystreams-core/) specification.

