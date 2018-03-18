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
package org.trellisldp.http;

import static java.util.Arrays.asList;
import static java.util.Base64.getEncoder;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD2;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * A {@link BinaryService} implementation that stores LDP-NR resources at HTTP
 * locations via {@code GET} and {@code PUT}.
 *
 * <p>This service supports the following digest algorithms:
 * <ul>
 * <li>MD5</li>
 * <li>MD2</li>
 * <li>SHA</li>
 * <li>SHA-1</li>
 * <li>SHA-256</li>
 * <li>SHA-384</li>
 * <li>SHA-512</li>
 * </ul>
 *
 * @author acoburn
 */
public class HttpBasedBinaryService implements BinaryService {

    private static final String NON_NULL_IDENTIFIER = "Identifier may not be null!";
    private static final String SHA = "SHA";

    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512).stream()
        .collect(toSet());

    private static final Logger LOGGER = getLogger(HttpBasedBinaryService.class);

    // TODO - JDK9 use the new HttpClient library
    private final Client httpClient;
    private final IdentifierService idService;

    /**
     * Create an Http-based binary service using the default HTTP client.
     *
     * @param idService an identifier service
     */
    public HttpBasedBinaryService(final IdentifierService idService) {
        this(idService, newClient());
    }

    /**
     * Create an Http-based binary service with a provided client.
     *
     * @param idService an identifier service
     * @param client the client
     */
    @Inject
    public HttpBasedBinaryService(final IdentifierService idService, final Client client) {
        requireNonNull(client, "HTTP client may not be null!");
        this.idService = idService;
        httpClient = client;
    }

    @Override
    public Boolean exists(final IRI identifier) {
        requireNonNull(identifier, NON_NULL_IDENTIFIER);
        final Response res = httpClient.target(identifier.getIRIString()).request().head();
        final Boolean status = res.getStatusInfo().getFamily().equals(SUCCESSFUL);
        LOGGER.debug("HTTP HEAD request to {} returned status {}", identifier, res.getStatus());
        res.close();
        return status;
    }

    @Override
    public Optional<InputStream> getContent(final IRI identifier, final List<Range<Integer>> ranges) {
        requireNonNull(identifier,  NON_NULL_IDENTIFIER);
        requireNonNull(ranges, "Byte ranges may not be null");
        final Response res;
        if (ranges.isEmpty()) {
            res = httpClient.target(identifier.getIRIString()).request().get();
        } else {
            final StringBuilder builder = new StringBuilder();
            ranges.forEach(r -> builder.append(r.getMinimum() + "-" + r.getMaximum()));
            res = httpClient.target(identifier.getIRIString()).request().header("Range", "bytes=" + builder).get();
        }
        LOGGER.debug("HTTP GET request to {} returned status {}", identifier, res.getStatus());
        if (res.hasEntity()) {
            return of(res.getEntity()).map(x -> (InputStream) x);
        }
        return empty();
    }

    @Override
    public void setContent(final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        requireNonNull(identifier, NON_NULL_IDENTIFIER);
        final Response res = httpClient.target(identifier.getIRIString()).request().put(entity(stream,
                    ofNullable(metadata.get(CONTENT_TYPE)).map(MediaType::valueOf)
                        .orElse(APPLICATION_OCTET_STREAM_TYPE)));
        LOGGER.debug("HTTP PUT request to {} returned {}", identifier, res.getStatusInfo());
        final Boolean ok = res.getStatusInfo().getFamily().equals(SUCCESSFUL);
        res.close();
        if (!ok) {
            throw new RuntimeTrellisException("HTTP PUT request to " + identifier + " failed with a " +
                    res.getStatusInfo());
        }
    }

    @Override
    public void purgeContent(final IRI identifier) {
        requireNonNull(identifier, NON_NULL_IDENTIFIER);
        final Response res = httpClient.target(identifier.getIRIString()).request().delete();
        LOGGER.debug("HTTP DELETE request to {} returned {}", identifier, res.getStatusInfo());
        final Boolean ok = res.getStatusInfo().getFamily().equals(SUCCESSFUL);
        res.close();
        if (!ok) {
            throw new RuntimeTrellisException("HTTP DELETE request to " + identifier + " failed with a " +
                    res.getStatusInfo());

        }
    }

    @Override
    public String generateIdentifier() {
        return idService.getSupplier().get();
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public Optional<String> digest(final String algorithm, final InputStream stream) {
        if (SHA.equals(algorithm)) {
            return of(SHA_1).map(DigestUtils::getDigest).flatMap(computeDigest(stream));
        }
        return ofNullable(algorithm).filter(supportedAlgorithms()::contains).map(DigestUtils::getDigest)
            .flatMap(computeDigest(stream));
    }

    private Function<MessageDigest, Optional<String>> computeDigest(final InputStream stream) {
        return algorithm -> {
            try {
                final String digest = getEncoder().encodeToString(DigestUtils.updateDigest(algorithm, stream).digest());
                stream.close();
                return of(digest);
            } catch (final IOException ex) {
                LOGGER.error("Error computing digest: {}", ex.getMessage());
            }
            return empty();
        };
    }
}
