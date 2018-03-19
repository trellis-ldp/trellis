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
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.awaitility.Awaitility.await;
import static org.trellisldp.api.RDFUtils.getInstance;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.LDP;

/**
 * Common utility functions.
 */
public final class TestUtils {

    private static IOService ioService = new JenaIOService(new NoopNamespaceService());

    /**
     * Build a JWT Token.
     * @param webid the web ID
     * @param secret the JWT secret
     * @return the JWT token
     */
    public static String buildJwt(final String webid, final String secret) {
        return "Bearer " + Jwts.builder().claim("webid", webid)
            .signWith(SignatureAlgorithm.HS512, secret.getBytes(UTF_8)).compact();
    }

    /**
     * Get the IO service.
     * @return the I/O service
     */
    public static IOService getIOService() {
        return ioService;
    }


    /**
     * Read an entity as an RDF Graph.
     * @param entity the HTTP entity
     * @param baseURL the base URL
     * @param syntax the RDF syntax
     * @return the graph
     */
    public static Graph readEntityAsGraph(final Object entity, final String baseURL,
            final RDFSyntax syntax) {
        final Graph g = getInstance().createGraph();
        getIOService().read((InputStream) entity, baseURL, syntax).forEach(g::add);
        return g;
    }

    /**
     * Read an http entity as a string.
     * @param entity the entity
     * @return the entity as a string
     */
    public static String readEntityAsString(final Object entity) {
        try {
            return IOUtils.toString((InputStream) entity, UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Test if the given ldp:constrainedBy link is present.
     * @param iri the IRI
     * @return true if present; false otherwise
     */
    public static Predicate<Link> hasConstrainedBy(final IRI iri) {
        return link -> LDP.constrainedBy.getIRIString().equals(link.getRel())
            && iri.getIRIString().equals(link.getUri().toString());
    }

    /**
     * Test if the given type link is present.
     * @param iri the IRI
     * @return true if present; false otherwise
     */
    public static Predicate<Link> hasType(final IRI iri) {
        return link -> "type".equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    /**
     * Get a resource path.
     * @param path the path
     * @return the absolute path for a resource
     */
    public static String getResourcePath(final String path) {
        try {
            return new File(CommonTests.class.getResource(path).toURI()).getAbsolutePath();
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get a resource as a string.
     * @param path the resource path
     * @return the resource as a string
     */
    public static String getResourceAsString(final String path) {
        try {
            return IOUtils.toString(CommonTests.class.getResourceAsStream(path), UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Get the link headers from a response.
     * @param res the response
     * @return a list of links
     */
    public static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        return res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList());
    }

    /**
     * Add in a delay.
     * @return an instant
     */
    public static Instant meanwhile() {
        final Instant t1 = now();
        await().until(() -> isReallyLaterThan(t1));
        final Instant t2 = now();
        await().until(() -> isReallyLaterThan(t2));
        return t2;
    }

    /**
     * Check that it is now really later than the provided instant.
     * @param time an instant
     * @return true if it is now later than the provided instant; false otherwise
     */
    public static Boolean isReallyLaterThan(final Instant time) {
        final Instant t = now();
        return t.isAfter(time) && t.getEpochSecond() > time.getEpochSecond();
    }

    private TestUtils() {
        // prevent instantiation
    }
}
