/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.LINK;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Predicate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.jena.JenaRDF;
import org.trellisldp.api.IOService;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.LDP;

class BaseCommonTests {

    protected static final String PATCH = "PATCH";

    private static String BASE_URL = null;
    private static Client CLIENT = null;

    private static final IOService IO_SVC = new JenaIOService(
            new NamespacesJsonContext(getResourcePath("/data/namespaces.json")));
    protected static final RDF rdf = new JenaRDF();

    protected static void setUp() throws Exception {
        // no-op by default
    }

    protected static void tearDown() throws Exception {
        // no-op by default
    }

    protected static void setBaseUrl(final String baseUrl) {
        requireNonNull(baseUrl, "baseUrl may not be null!");
        BASE_URL = baseUrl;
    }

    protected static void setClient(final Client client) {
        requireNonNull(client, "client may not be null!");
        CLIENT = client;
        CLIENT.property("jersey.config.client.connectTimeout", 5000);
        CLIENT.property("jersey.config.client.readTimeout", 5000);
    }

    protected static Client getClient() {
        requireNonNull(CLIENT, "HTTP Client has not been initialized!");
        return CLIENT;
    }

    protected static String getBaseUrl() {
        requireNonNull(BASE_URL, "BaseUrl has not been initialized!");
        return BASE_URL;
    }

    protected static WebTarget target() {
        return target(getBaseUrl());
    }

    protected static WebTarget target(final String url) {
        return getClient().target(url);
    }

    protected static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        return res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList());
    }

    protected static String getResourcePath(final String path) {
        try {
            return new File(BaseCommonTests.class.getResource(path).toURI()).getAbsolutePath();
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static String getResourceAsString(final String path) {
        try {
            return IOUtils.toString(BaseCommonTests.class.getResourceAsStream(path), UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    protected Predicate<Link> hasConstrainedBy(final IRI iri) {
        return link -> LDP.constrainedBy.getIRIString().equals(link.getRel())
            && iri.getIRIString().equals(link.getUri().toString());
    }

    protected Predicate<Link> hasType(final IRI iri) {
        return link -> "type".equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    protected static Graph readEntityAsGraph(final Object entity, final RDFSyntax syntax) {
        final Graph g = rdf.createGraph();
        IO_SVC.read((InputStream) entity, getBaseUrl(), syntax).forEach(g::add);
        return g;
    }

    protected static String readEntityAsString(final Object entity) {
        try {
            return IOUtils.toString((InputStream) entity, UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    protected static String buildJwt(final String webid, final String secret) {
        return "Bearer " + Jwts.builder().claim("webid", webid)
            .signWith(SignatureAlgorithm.HS512, secret.getBytes(UTF_8)).compact();
    }
}
