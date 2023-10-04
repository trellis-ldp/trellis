# WebDAV extension for Trellis

This module implements WebDAV HTTP methods as JAX-RS beans. As resource matchers and filters, they can be
added to a Trellis application if WebDAV support is desired.

If the WebDAV module is used in combination with WebAC authorization, one will need set some configuration
properties, informing the WebAC module of the WebDAV methods.

For example, the following values may be placed into a `./META-INF/microprofile-config.properties` file

    trellis.webac.method.readable=PROPFIND
    trellis.webac.method.writable=PROPPATCH,COPY,MOVE
    trellis.webac.method.appendable=MKCOL

Alternatively, these properties may be set in the environment (e.g. in `JAVA_OPTS` or as system properties).


