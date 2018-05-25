# Deployable Trellis Application

This module assembles Trellis into a deployable archive.

## Requirements

  * Java 8 or later

## Running Trellis

Unpack a zip or tar distribution. In that directory, modify `./etc/config.yml` to match the
desired values for your system.

To run trellis directly from within a console, issue this command:

```bash
$ ./bin/trellis server ./etc/config.yml
```

## Installation

To install Trellis as a [`systemd`](https://en.wikipedia.org/wiki/Systemd) service on linux,
follow the steps below. `systemd` is used by linux distributions such as CentOS/RHEL 7+ and Ubuntu 15+.

1. Move the unpacked Trellis directory to a location such as `/opt/trellis`.
   If you choose a different location, please update the `./etc/trellis.service` script.

2. Edit the `./etc/environment` file as desired (optional).

3. Edit the `./etc/config.yml` file as desired (optional).

4. Create a trellis user:

```bash
$ sudo useradd -r trellis -s /sbin/nologin
```

5. Create data directories. A different location can be used, but then please update
   the `./etc/config.yml` file.

```bash
$ sudo mkdir /var/lib/trellis
$ sudo chown trellis.trellis /var/lib/trellis
```

6. Install the systemd file:

```bash
$ sudo ln -s /opt/trellis/etc/trellis.service /etc/systemd/system/trellis.service
```

7. Reload systemd to see the changes

```bash
$ sudo systemctl daemon-reload
```

8. Start the trellis service

```bash
$ sudo systemctl start trellis
```

To check that trellis is running, check the URL: `http://localhost:8080`

Application health checks are available at `http://localhost:8081/healthcheck`

## Building Trellis

1. Run `./gradlew assemble` to build the application or download one of the releases.
2. Unpack the appropriate distribution in `./platform/linux/build/distributions`
3. Start the application according to the steps above

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
| allowHeaders | "Content-Type", "Link", "Accept", "Accept-Datetime", "Prefer", "Want-Digest", "Slug", "Digest" | A list of allowed request headers |
| exposeHeaders | "Content-Type", "Link", "Memento-Datetime", "Preference-Applied", "Location", "Accept-Patch", "Accept-Post", "Digest", "Accept-Ranges", "ETag", "Vary" | A list of allowed response headers |
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
    contextWhitelist:
        - "http://example.com/context.json"
    contextDomainWhitelist:
        - "http://example.com/"
    cacheExpireHours: 24
    cacheSize: 100
```

| Name | Default | Description |
| ---- | ------- | ----------- |
| contextWhitelist | an empty list | a user-supplied whitelist of valid JSON-LD profile values |
| contextDomainWhitelist | an empty list | a user-supplied whitelist of domains for valid JSON-LD profile values |
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

#### HTTP/2 Java Implementation Notes:
To use HTTP/2 over TLS with JRE 1.8 requires the [alpn-boot](https://mvnrepository.com/artifact/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117) library to be in the bootclasspath.

If using the Linux distribution, you can set an environment variable:

```bash
JAVA_OPTS=-Xbootclasspath/p:/path/to/alpn-boot.jar
```

For JDK 9+, you can simply add a dependency like this to the `trellis-app-triplestore` build.gradle:

```groovy
jettyAlpnServerVersion = '9.4.8.v20171121'
compile("org.eclipse.jetty:jetty-alpn-java-server:$jettyAlpnServerVersion")
```

## Metrics reporting

Application metrics can be configured by defining a frequency and a reporter. For more information about configuring these
reports, please refer to the [dropwizard metrics configuration reference](http://www.dropwizard.io/1.0.5/docs/manual/configuration.html#metrics).

