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
package org.trellisldp.http.impl;

import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static org.apache.commons.lang3.Range.between;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.domain.HttpConstants.DIGEST;
import static org.trellisldp.http.domain.HttpConstants.LINK_TEMPLATE;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.HttpConstants.RANGE;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.Prefer.PREFER_MINIMAL;
import static org.trellisldp.http.domain.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.domain.Prefer.PREFER_RETURN;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.filterWithLDF;
import static org.trellisldp.http.impl.RdfUtils.filterWithPrefer;
import static org.trellisldp.http.impl.RdfUtils.getDefaultProfile;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.unskolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.http.domain.Range;
import org.trellisldp.http.domain.Version;
import org.trellisldp.http.domain.WantDigest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;

/**
 * The GET response builder.
 *
 * @author acoburn
 */
public class GetHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(GetHandler.class);

    private final IOService ioService;
    private final BinaryService binaryService;

    /**
     * A GET response builder.
     *
     * @param req the LDP request
     * @param resourceService the resource service
     * @param ioService the serialization service
     * @param binaryService the binary service
     * @param baseUrl the base URL
     */
    public GetHandler(final LdpRequest req, final ResourceService resourceService, final IOService ioService,
            final BinaryService binaryService, final String baseUrl) {
        super(req, resourceService, null, baseUrl);
        this.ioService = ioService;
        this.binaryService = binaryService;
    }

    /**
     * Build the representation for the given resource.
     *
     * @param res the resource
     * @return the response builder
     */
    public ResponseBuilder getRepresentation(final Resource res) {
        final String identifier = getBaseUrl() + req.getPath();

        // Check if this is already deleted
        checkDeleted(res, identifier);

        LOGGER.debug("Acceptable media types: {}", req.getHeaders().getAcceptableMediaTypes());
        final Optional<RDFSyntax> syntax = getSyntax(ioService, req.getHeaders().getAcceptableMediaTypes(),
                res.getBinary().filter(b -> !DESCRIPTION.equals(req.getExt()))
                               .map(b -> b.getMimeType().orElse(APPLICATION_OCTET_STREAM)));

        if (ACL.equals(req.getExt()) && !res.hasAcl()) {
            throw new NotFoundException();
        }

        final ResponseBuilder builder = basicGetResponseBuilder(res, syntax);

        // Add NonRDFSource-related "describe*" link headers
        res.getBinary().ifPresent(ds -> {
            final String base = getBaseBinaryIdentifier(identifier);
            final String sep = base.contains("?") ? "&" : "?";
            if (syntax.isPresent()) {
                builder.link(base + sep + "ext=description", "canonical").link(base, "describes");
            } else {
                builder.link(base, "canonical").link(base + sep + "ext=description", "describedby")
                    .type(ds.getMimeType().orElse(APPLICATION_OCTET_STREAM));
            }
        });

        // Add a "self" link header
        builder.link(getSelfIdentifier(identifier), "self");

        // Only show memento links for the user-managed graph (not ACL)
        if (!ACL.equals(req.getExt())) {
            builder.link(identifier, "original timegate")
                .links(MementoResource.getMementoLinks(identifier, resourceService.getMementos(res.getIdentifier()))
                        .toArray(Link[]::new));
        }

        // URI Template
        builder.header(LINK_TEMPLATE, "<" + identifier + "{?version}>; rel=\"" + Memento.Memento.getIRIString() + "\"");

        // NonRDFSources responses (strong ETags, etc)
        if (res.getBinary().isPresent() && !syntax.isPresent()) {
            return getLdpNr(identifier, res, builder);
        }

        // RDFSource responses (weak ETags, etc)
        final RDFSyntax s = syntax.orElse(TURTLE);
        final IRI profile = getProfile(req.getHeaders().getAcceptableMediaTypes(), s);
        return getLdpRs(identifier, res, builder, s, profile);
    }

    private String getSelfIdentifier(final String identifier) {
        // Add any version or ext parameters
        if (nonNull(req.getVersion()) || ACL.equals(req.getExt())) {
            final List<String> query = new ArrayList<>();

            ofNullable(req.getVersion()).map(Version::getInstant).map(Instant::toEpochMilli).map(x -> "version=" + x)
                .ifPresent(query::add);

            if (ACL.equals(req.getExt())) {
                query.add("ext=acl");
            } else if (DESCRIPTION.equals(req.getExt())) {
                query.add("ext=description");
            }
            return identifier + "?" + join("&", query);
        }
        return identifier;
    }

    private String getBaseBinaryIdentifier(final String identifier) {
        // Add the version parameter, if present
        return identifier + ofNullable(req.getVersion()).map(Version::getInstant).map(Instant::toEpochMilli)
                .map(x -> "?version=" + x).orElse("");
    }

    private ResponseBuilder getLdpRs(final String identifier, final Resource res, final ResponseBuilder builder,
            final RDFSyntax syntax, final IRI profile) {

        final Prefer prefer = ACL.equals(req.getExt()) ?
            new Prefer(PREFER_REPRESENTATION, singletonList(PreferAccessControl.getIRIString()),
                    of(PreferUserManaged, LDP.PreferContainment, LDP.PreferMembership).map(IRI::getIRIString)
                        .collect(toList()), null, null, null) : req.getPrefer();

        // Check for a cache hit
        final EntityTag etag = new EntityTag(buildEtagHash(identifier, res.getModified(), prefer), true);
        checkCache(req.getRequest(), res.getModified(), etag);

        builder.tag(etag);
        if (res.isMemento()) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else if (ACL.equals(req.getExt())) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH));
        } else if (res.getInteractionModel().equals(LDP.RDFSource)) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE));
        } else {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE, POST));
        }

        // URI Templates
        builder.header(LINK_TEMPLATE, "<" + identifier + "{?subject,predicate,object}>; rel=\""
                + LDP.RDFSource.getIRIString() + "\"");

        ofNullable(prefer).ifPresent(p -> builder.header(PREFERENCE_APPLIED, PREFER_RETURN + "=" + p.getPreference()
                    .orElse(PREFER_REPRESENTATION)));


        if (ofNullable(prefer).flatMap(Prefer::getPreference).filter(PREFER_MINIMAL::equals).isPresent()) {
            return builder.status(NO_CONTENT);
        }

        // Short circuit HEAD requests
        if (HEAD.equals(req.getRequest().getMethod())) {
            return builder;
        }

        // Stream the rdf content
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream out) throws IOException {
                try (final Stream<? extends Quad> stream = res.stream()) {
                    ioService.write(stream.filter(filterWithPrefer(prefer))
                        .map(unskolemizeQuads(resourceService, getBaseUrl()))
                        .filter(filterWithLDF(req.getSubject(), req.getPredicate(), req.getObject()))
                        .map(Quad::asTriple), out, syntax,
                            ofNullable(profile).orElseGet(() -> getDefaultProfile(syntax, identifier)));
                }
            }
        };
        return builder.entity(stream);
    }

    private ResponseBuilder getLdpNr(final String identifier, final Resource res, final ResponseBuilder builder) {

        final Instant mod = res.getBinary().map(Binary::getModified).orElseThrow(() ->
                new WebApplicationException("Could not access binary metadata for " + res.getIdentifier()));
        final EntityTag etag = new EntityTag(buildEtagHash(identifier + "BINARY", mod, null));
        checkCache(req.getRequest(), mod, etag);

        // Set last-modified to be the binary's last-modified value
        builder.lastModified(from(mod));

        final IRI dsid = res.getBinary().map(Binary::getIdentifier).orElseThrow(() ->
                new WebApplicationException("Could not access binary metadata for " + res.getIdentifier()));

        builder.header(VARY, RANGE).header(VARY, WANT_DIGEST).header(ACCEPT_RANGES, "bytes").tag(etag);

        if (res.isMemento()) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PUT, DELETE));
        }

        // Add instance digests, if Requested and supported
        ofNullable(req.getWantDigest()).map(WantDigest::getAlgorithms).ifPresent(algs ->
                algs.stream().filter(binaryService.supportedAlgorithms()::contains).findFirst().ifPresent(alg ->
                    getBinaryDigest(dsid, alg).ifPresent(digest ->
                        builder.header(DIGEST, alg.toLowerCase() + "=" + digest))));

        // Stream the binary content
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream out) throws IOException {
                // TODO -- with JDK 9 use InputStream::transferTo instead of IOUtils::copy
                try (final InputStream binary = getBinaryStream(dsid, req.getRange())) {
                    IOUtils.copy(binary, out);
                } catch (final IOException ex) {
                    LOGGER.error("Error writing binary content", ex);
                    throw new WebApplicationException("Error processing binary content: " +
                            ex.getMessage());
                }
            }
        };

        return builder.entity(stream);
    }

    private InputStream getBinaryStream(final IRI dsid, final Range range) throws IOException {
        final Optional<InputStream> content = isNull(range)
            ? binaryService.getContent(dsid)
            : binaryService.getContent(dsid, singletonList(between(range.getFrom(), range.getTo())));
        return content.orElseThrow(() -> new IOException("Could not retrieve content from: " + dsid));
    }

    private Optional<String> getBinaryDigest(final IRI dsid, final String algorithm) {
        final Optional<InputStream> b = binaryService.getContent(dsid);
        try (final InputStream is = b.orElseThrow(() -> new WebApplicationException("Couldn't fetch binary content"))) {
            return binaryService.digest(algorithm, is);
        } catch (final IOException ex) {
            LOGGER.error("Error computing digest on content", ex);
            throw new WebApplicationException("Error handling binary content: " + ex.getMessage());
        }
    }

    private ResponseBuilder basicGetResponseBuilder(final Resource res, final Optional<RDFSyntax> syntax) {
        final ResponseBuilder builder = ok();

        // Standard HTTP Headers
        builder.lastModified(from(res.getModified())).header(VARY, ACCEPT);

        final IRI model;

        if (isNull(req.getExt()) || DESCRIPTION.equals(req.getExt())) {
            syntax.ifPresent(s -> {
                builder.header(VARY, PREFER);
                builder.type(s.mediaType());
            });

            model = res.getBinary().isPresent() && syntax.isPresent() ? LDP.RDFSource : res.getInteractionModel();
            // Link headers from User data
            res.getExtraLinkRelations().collect(toMap(Entry::getKey, Entry::getValue))
                .entrySet().forEach(entry -> builder.link(entry.getKey(), join(" ", entry.getValue())));
        } else {
            model = LDP.RDFSource;
        }

        // Add LDP-required headers
        ldpResourceTypes(model).forEach(type -> {
            builder.link(type.getIRIString(), "type");
            // Mementos don't accept POST or PATCH
            if (LDP.Container.equals(type) && !res.isMemento()) {
                builder.header(ACCEPT_POST, ioService.supportedWriteSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            } else if (LDP.RDFSource.equals(type) && !res.isMemento()) {
                builder.header(ACCEPT_PATCH, ioService.supportedUpdateSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            }
        });

        // Memento-related headers
        if (res.isMemento()) {
            builder.header(MEMENTO_DATETIME, from(res.getModified()));
        } else {
            builder.header(VARY, ACCEPT_DATETIME);
        }

        return builder;
    }
}
