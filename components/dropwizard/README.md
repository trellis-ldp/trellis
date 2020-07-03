# Trellis Dropwizard Application

This module forms that basis for a deployable Trellis application, based on Dropwizard.

## Configuration

The web application wrapper (Dropwizard.io) makes many [configuration options](http://www.dropwizard.io/1.2.0/docs/manual/configuration.html)
available. Any of the configuration options defined by Dropwizard can be part of your application's configuration file.

Trellis defines its own configuration options, including:

```yaml
resources: /path/to/resources
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| resources | (none) | The path for storing resources. If not defined, an in-memory dataset will be used. If this value begins with `http://` or `https://`, a remote triplestore will be used. |

```yaml
binaries: /path/to/binaries
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| binaries | (none) | The path for storing binaries |

```yaml
mementos: /path/to/mementos
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| mementos | (none) | The path for storing mementos |

```yaml
baseUrl: http://localhost:8080/
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| baseUrl | (none) | A defined baseUrl for resources in this partition. If not defined, the `Host` request header will be used |

```yaml
namespaces: /path/to/namespaces.json
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| namespaces | (none) | The path to a JSON file defining namespace prefixes |

```yaml
auth:
    adminUsers: []
    webac:
        enabled: true
    anon:
        enabled: true
    jwt:
        enabled: true
        base64Encoded: false
        key: secret
    basic:
        enabled: true
        usersFile: /path/to/users.auth
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| adminUsers | (none) | A list of webIDs that should be given admin access for the purpose of authorization |
| webac / enabled | true | Whether WebAC authorization is enabled |
| anon / enabled | false | Whether anonymous authentication is enabled |
| jwt / enabled | true | Whether jwt authentication is enabled |
| jwt / base64Encoded | false | Whether the key is base64 encoded |
| jwt / key | (none) | The signing key for JWT tokens |
| basic / enabled | true | Whether basic authentication is enabled |
| basic / usersFile | (none) | The path to a file where user credentials are stored |

```yaml
cors:
    enabled: true
    allowOrigin:
        - "*"
    allowMethods:
        - "GET"
        - "POST"
        - "PATCH"
    allowHeaders:
        - "Content-Type"
        - "Link"
    exposeHeaders:
        - "Link"
        - "Location"
    maxAge: 180
    allowCredentials: true
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| enabled | false | Whether [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) is enabled |
| allowOrigin | "*" | A list of allowed origins |
| allowMethods | "PUT", "DELETE", "PATCH", "GET", "HEAD", "OPTIONS", "POST" | A list of allowed methods |
| allowHeaders | "Content-Type", "Link", "Accept", "Accept-Datetime", "Prefer", "Slug", "Authorization"  | A list of allowed request headers |
| exposeHeaders | "Content-Type", "Link", "Memento-Datetime", "Preference-Applied", "Location", "Accept-Patch", "Accept-Post", "Accept-Ranges", "ETag", "Vary" | A list of allowed response headers |
| maxAge | 180 | The maximum age (in seconds) of pre-flight messages |
| allowCredentials | true | Whether the actual request can be made with credentials |

```yaml
cache:
    maxAge: 86400
    mustRevalidate: true
    noCache: false
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| maxAge | 86400 | The value of the `Cache-Control: max-age=` response header |
| mustRevalidate | true | Whether to include a `Cache-Controle: must-revalidate` directive |
| noCache | false | Whether to include a `Cache-Control: no-cache` directive |

```yaml
jsonld:
    allowedContexts:
        - "http://example.com/context.json"
    allowedContextDomains:
        - "http://example.com/"
    cacheExpireHours: 24
    cacheSize: 100
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| allowedContexts | an empty list | a user-supplied list of valid JSON-LD profile values |
| allowedContextDomains | an empty list | a user-supplied list of domains for valid JSON-LD profile values |
| cacheExpireHours | 24 | The number of hours that a JSON-LD profile value will be stored in a cache. |
| cacheSize | 100 | The number of entries stored in the JSON-LD profile cache. |

## Alternate HTML Representation
The Trellis RDFa module includes a configurable HTMLSerializer.  It accepts the following optional asset configuration options.

For example, to configure the HTML to produce a table, one can opt to use the `resource-table.mustache` template.  
With this tabular HTML, one could then add sorting and search filter functionality with a JS library.

```yaml
assets:
    template: org/trellisldp/rdfa/resource-table.mustache
    icon: "http://example.org/image.icon"
    js:
        - "https://unpkg.com/vanilla-datatables@latest/dist/vanilla-dataTables.min.js"
        - "http://example.org/table.js"
    css:
        - "http://example.org/styles1.css"
        - "http://example.org/styles2.css"
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| template | org/trellisldp/rdfa/resource.mustache | an HTML template located in the `org.trellisldp.rdfa` module classpath |
| icon | none | a URL to a favicon |
| js | none | a list of URLs for javascript assets |
| css | none | a list of URLs for CSS assets |

## HTTP/2

Trellis supports the [HTTP/2 protocol](https://http2.github.io/faq/). For more information about how to enable HTTP/2 with Trellis,
please refer to the [dropwizard documentation](http://www.dropwizard.io/1.2.0/docs/manual/configuration.html#http-2-over-tls).
In particular, an `h2` connector type must be used, typically with a corresponding TLS configuration.

## Metrics reporting

Application metrics can be configured by defining a frequency and a reporter. For more information about configuring these
reports, please refer to the [dropwizard metrics configuration reference](http://www.dropwizard.io/1.0.5/docs/manual/configuration.html#metrics).

