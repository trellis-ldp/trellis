/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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

import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.notModified;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.*;
import static org.trellisldp.http.core.HttpConstants.*;
import static org.trellisldp.vocabulary.JSONLD.compacted;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.IOService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Http Utility functions.
 *
 * @author acoburn
 */
public final class HttpUtils {

    private static final Logger LOGGER = getLogger(HttpUtils.class);
    private static final RDF rdf = RDFFactory.getInstance();
    private static final Set<IRI> ignoredPreferences;

    static {
        final Set<IRI> ignore = new HashSet<>();
        ignore.add(Trellis.PreferUserManaged);
        ignore.add(Trellis.PreferServerManaged);
        ignoredPreferences = unmodifiableSet(ignore);
    }

    /**
     * Get all of the LDP resource (super) types for the given LDP interaction model.
     *
     * @param ixnModel the interaction model
     * @return a stream of types
     */
    public static Stream<IRI> ldpResourceTypes(final IRI ixnModel) {
        final Stream.Builder<IRI> supertypes = Stream.builder();
        if (ixnModel != null) {
            LOGGER.trace("Finding types that subsume {}", ixnModel.getIRIString());
            supertypes.accept(ixnModel);
            final IRI superClass = LDP.getSuperclassOf(ixnModel);
            LOGGER.trace("... including {}", superClass);
            ldpResourceTypes(superClass).forEach(supertypes);
        }
        return supertypes.build();
    }

    /**
     * Test matching an identifier, irrespective of trailing slash.
     * @param subject the subject of a triple
     * @param identifier the identifier
     * @return a triple predicate
     */
    public static boolean matchIdentifier(final BlankNodeOrIRI subject, final IRI identifier) {
        if (subject.equals(identifier)) {
            return true;
        }
        return subject instanceof IRI && ((IRI) subject).getIRIString().equals(identifier.getIRIString() + "/");
    }

    /**
     * Get a collection of IRIs for identifying the categories of triples to retrieve.
     *
     * @param prefer the Prefer header
     * @return the categories of triples to retrieve
     */
    public static Set<IRI> triplePreferences(final Prefer prefer) {
        final Set<IRI> include = new HashSet<>(DEFAULT_REPRESENTATION);
        if (prefer != null) {
            if (prefer.getInclude().contains(LDP.PreferMinimalContainer.getIRIString())) {
                include.remove(LDP.PreferContainment);
                include.remove(LDP.PreferMembership);
            }
            if (prefer.getOmit().contains(LDP.PreferMinimalContainer.getIRIString())) {
                include.remove(Trellis.PreferUserManaged);
                include.remove(Trellis.PreferServerManaged);
            }
            prefer.getOmit().stream().map(rdf::createIRI).forEach(include::remove);
            prefer.getInclude().stream().map(rdf::createIRI)
                .filter(iri -> !ignoredPreferences.contains(iri)).forEach(include::add);
        }
        return include;
    }

    /**
     * Convert triples from a skolemized form to an externa form.
     *
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> unskolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toExternal(svc.unskolemize(triple.getSubject()),
                    baseUrl), triple.getPredicate(), svc.toExternal(svc.unskolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert triples from an external form to a skolemized form.
     *
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> skolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toInternal(svc.skolemize(triple.getSubject()), baseUrl),
                triple.getPredicate(), svc.toInternal(svc.skolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert quads from an external form to a skolemized form.
     *
     * @param svc the resource service
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Quad, Quad> skolemizeQuads(final ResourceService svc, final String baseUrl) {
        return quad -> rdf.createQuad(quad.getGraphName().orElse(Trellis.PreferUserManaged),
                (BlankNodeOrIRI) svc.toInternal(svc.skolemize(quad.getSubject()), baseUrl), quad.getPredicate(),
                svc.toInternal(svc.skolemize(quad.getObject()), baseUrl));
    }

    /**
     * Given a list of acceptable media types, get an RDF syntax.
     *
     * @param ioService the I/O service
     * @param acceptableTypes the types from HTTP headers
     * @param mimeType an additional "default" mimeType to match
     * @return an RDFSyntax or, in the case of binaries, null
     * @throws NotAcceptableException if no acceptable syntax is available
     */
    public static RDFSyntax getSyntax(final IOService ioService, final List<MediaType> acceptableTypes,
            final String mimeType) {
        if (acceptableTypes.isEmpty()) {
            return mimeType != null ? null : TURTLE;
        }
        final MediaType mt = mimeType != null ? MediaType.valueOf(mimeType) : null;
        for (final MediaType type : acceptableTypes) {
            if (type.isCompatible(mt)) {
                return null;
            }
            final RDFSyntax syntax = ioService.supportedReadSyntaxes().stream()
                .filter(s -> MediaType.valueOf(s.mediaType()).isCompatible(type))
                .findFirst().orElse(null);
            if (syntax != null) {
                return syntax;
            }
        }
        LOGGER.debug("Valid syntax not found among {} or {}", acceptableTypes, mimeType);
        throw new NotAcceptableException();
    }

    /**
     * Given a list of acceptable media types and an RDF syntax, get the relevant profile data, if
     * relevant.
     *
     * @param acceptableTypes the types from HTTP headers
     * @param syntax an RDF syntax
     * @return a profile IRI if relevant
     */
    public static IRI getProfile(final List<MediaType> acceptableTypes, final RDFSyntax syntax) {
        for (final MediaType type : acceptableTypes) {
            if (RDFSyntax.byMediaType(type.toString()).filter(isEqual(syntax)).isPresent() &&
                    type.getParameters().containsKey("profile")) {
                return rdf.createIRI(strip(type.getParameters().get("profile"), "\"").split(" ")[0].trim());
            }
        }
        return null;
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier.
     *
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @param defaultJsonLdProfile a user-supplied default
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final String identifier,
            final String defaultJsonLdProfile) {
        return getDefaultProfile(syntax, rdf.createIRI(identifier), defaultJsonLdProfile);
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier.
     *
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @param defaultJsonLdProfile a user-supplied default
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final IRI identifier,
            final String defaultJsonLdProfile) {
        if (RDFA.equals(syntax)) {
            return identifier;
        } else if (defaultJsonLdProfile != null) {
            return rdf.createIRI(defaultJsonLdProfile);
        }
        return compacted;
    }

    /**
     * Check whether an LDP type is a sort of container.
     * @param ldpType the LDP type to test
     * @return true if it is a type of LDP container
     */
    public static boolean isContainer(final IRI ldpType) {
        return LDP.Container.equals(ldpType) || LDP.BasicContainer.equals(ldpType)
            || LDP.DirectContainer.equals(ldpType) || LDP.IndirectContainer.equals(ldpType);
    }

    /**
     * Check for a conditional operation.
     * @param method the HTTP method
     * @param ifModifiedSince the If-Modified-Since header
     * @param modified the resource modification date
     */
    public static void checkIfModifiedSince(final String method, final String ifModifiedSince,
            final Instant modified) {
        if (isGetOrHead(method)) {
            final Instant time = parseDate(ifModifiedSince);
            if (time != null && time.isAfter(modified.truncatedTo(SECONDS))) {
                throw new RedirectionException(notModified().build());
            }
        }
    }

    /**
     * Check for a conditional operation.
     * @param ifUnmodifiedSince the If-Unmodified-Since header
     * @param modified the resource modification date
     */
    public static void checkIfUnmodifiedSince(final String ifUnmodifiedSince, final Instant modified) {
        final Instant time = parseDate(ifUnmodifiedSince);
        if (time != null && modified.truncatedTo(SECONDS).isAfter(time)) {
            throw new ClientErrorException(status(PRECONDITION_FAILED).build());
        }
    }

    /**
     * Check for a conditional operation.
     * @param ifMatch the If-Match header
     * @param etag the resource etag
     */
    public static void checkIfMatch(final String ifMatch, final EntityTag etag) {
        if (ifMatch == null) {
            return;
        }
        final Set<String> items = stream(ifMatch.split(",")).map(String::trim).collect(toSet());
        if (items.contains("*")) {
            return;
        }
        try {
            if (etag.isWeak() || items.stream().map(EntityTag::valueOf).noneMatch(isEqual(etag))) {
                    throw new ClientErrorException(status(PRECONDITION_FAILED).build());
            }
        } catch (final IllegalArgumentException ex) {
            throw new BadRequestException(ex);
        }
    }

    /**
     * Check for a conditional operation.
     * @param method the HTTP method
     * @param ifNoneMatch the If-None-Match header
     * @param etag the resource etag
     */
    public static void checkIfNoneMatch(final String method, final String ifNoneMatch, final EntityTag etag) {
        if (ifNoneMatch == null) {
            return;
        }

        final Set<String> items = stream(ifNoneMatch.split(",")).map(String::trim).collect(toSet());
        if (isGetOrHead(method)) {
            if ("*".equals(ifNoneMatch) || items.stream().map(EntityTag::valueOf)
                    .anyMatch(e -> e.equals(etag) || e.equals(new EntityTag(etag.getValue(), !etag.isWeak())))) {
                throw new RedirectionException(notModified().build());
            }
        } else {
            if ("*".equals(ifNoneMatch) || items.stream().map(EntityTag::valueOf).anyMatch(isEqual(etag))) {
                throw new ClientErrorException(status(PRECONDITION_FAILED).build());
            }
        }
     }

    private static boolean isGetOrHead(final String method) {
        return "GET".equals(method) || "HEAD".equals(method);
    }

    private static Instant parseDate(final String date) {
        if (date != null) {
            try {
                return parse(date.trim(), RFC_1123_DATE_TIME).toInstant();
            } catch (final DateTimeException ex) {
                LOGGER.debug("Ignoring invalid date ({}): {}", date, ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Check whether conditional requests are required.
     * @param required whether conditional requests are required
     * @param ifMatch the If-Match header
     * @param ifUnmodifiedSince the If-Unmodified-Since header
     */
    public static void checkRequiredPreconditions(final boolean required, final String ifMatch,
            final String ifUnmodifiedSince) {
        if (required && ifMatch == null && ifUnmodifiedSince == null) {
            throw new ClientErrorException(status(PRECONDITION_REQUIRED).build());
        }
    }

    /**
     * Check whether a resource exists.
     * @param resource the resource
     * @return true if the resource isn't missing or deleted; false otherwise
     */
    public static boolean exists(final Resource resource) {
        return !DELETED_RESOURCE.equals(resource) && !MISSING_RESOURCE.equals(resource);
    }

    /**
     * Close a dataset.
     * @param dataset the dataset
     */
    public static void closeDataset(final Dataset dataset) {
        try {
            dataset.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing dataset", ex);
        }
    }

    /**
     * Build a set of non-metadata graph names.
     * @return a set of graph names
     */
    public static Set<IRI> buildIgnoredGraphNames() {
        final Set<IRI> graphs = new HashSet<>();
        graphs.add(LDP.PreferContainment);
        graphs.add(LDP.PreferMembership);
        graphs.add(Trellis.PreferServerManaged);
        graphs.add(Trellis.PreferUserManaged);
        return unmodifiableSet(graphs);
    }

    /**
     * Build a canonical url for a resource.
     * @param req the trellis request
     * @param baseUrl the base url
     * @return an absolute URL
     */
    public static String buildResourceUrl(final TrellisRequest req, final String baseUrl) {
        return fromUri(baseUrl).path(req.getPath() + (req.hasTrailingSlash() ? "/" : "")).build().toString();
    }

    private HttpUtils() {
        // prevent instantiation
    }
}
