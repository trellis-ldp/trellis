# trellis-webac

This module implements an Authorization layer for a Trellis server based on the W3C Web Access Control proposal.

This implementation conforms to the [SOLID WebAC specification](https://github.com/solid/solid-spec#authorization-and-access-control).

## WebAC authorization resources

Each Trellis resource has its own `acl` resource, which can be discovered by following a `Link: <https://example.org/resource?ext=acl>; rel="acl"` header.

This resource may or may not exist. If it doesn't exist, the WebAC enforcement algorithm will look for an ACL resource
in some parent resource until it reaches a partition's root resource.

When WebAC is in effect, that `?ext=acl` resource can only be accessed or modified if the given user agent has
the `acl:Control` privilege on the resource. If so, an `acl:Authorization` statement can be added to the resource:

```turtle
@prefix acl: <http://www.w3.org/ns/auth/acl#>.

<#authorization> a acl:Authorization ;
    acl:mode acl:Read, acl:Write ;
    acl:accessTo <https://example.org/resource> ;
    acl:agent <https://example.org/users/1>, <https://example.org/users/2> .
```

This will give `https://example.org/users/1` and `https://example.org/users/2` read and write access to the resource
at `https://example.org/resource`. Those users, however, will not be able to modify the ACL resource itself,
since they lack the `acl:Control` mode.

The other possible acl mode is `acl:Append`, which is somewhat like `acl:Write` except that, in the case of Trellis,
it only allows the addition of Containment or Membership triples to that resource. For instance, it would allow a
user to POST to a container but not modify the user-managed properties of that container or delete other child resources.

## Public access

It is possible to make a resource publicly accessible by adding the triple `<> acl:agentClass foaf:Agent`
to an Authorization statement. For example:

```turtle
@prefix acl: <http://www.w3.org/ns/auth/acl#>.
@prefix foaf: <http://xmlns.com/foaf/0.1/> .

<#authorization> a acl:Authorization;
    acl:agentClass foaf:Agent;  # everyone
    acl:mode acl:Read;  # has Read-only access
    acl:accessTo <https://example.org/resource> . # the resource
```

## Access for any authenticated agent

As with public access resources, it is possible to define the access controls on a resource for a class of agents such
that it is accessible to any authenticated user. That is, any agent that is not the `trellis:AnonymousAgent`.
For example:

```turtle
@prefix acl: <http://www.w3.org/ns/auth/acl#>.

<#authorization> a acl:Authorization;
    acl:agentClass acl:AuthenticatedAgent;  # everyone
    acl:mode acl:Read;  # has Read-only access
    acl:accessTo <https://example.org/resource> . # the resource
```

## Unsupported features of WebAC

Following the [Solid WebAC specification](https://github.com/solid/web-access-control-spec#not-supported-by-design) recommendation,
Trellis does not support `acl:accessToClass`, Regular Expressions in statements such as `acl:agentClass` or a strict notion of
resource ownership.
